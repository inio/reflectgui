package reflect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import util.Utils;
import util.REThrow;

public class Mirror {
	
	final static Pattern namePattern = Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*::)*[a-zA-Z_][a-zA-Z0-9_]*");

	public static final boolean braceLists = false;
	public static final boolean braceMaps = true;
	public static final boolean braceMapElements = false;
	
	static HashMap<String, Class<?>> getClassMap() {
		return NamedTypes.types;
	}
	
	private static List<MagicMirror<?>> magicMirrors = new ArrayList<MagicMirror<? extends Object>>();
	
	static {
		registerMirror(new ObjectMirror());
		registerMirror(new ReflectableMirror<Reflectable<?>>());
		registerMirror(new NumberMirror());
		registerMirror(new StringMirror());
		registerMirror(new FileMirror());
		registerMirror(new EnumMirror());
		registerMirror(new CollectionMirror());
		registerMirror(new MapMirror<Object,Object>());
	}
	
	/*static void registerClass(Class<?> c) {
		Reflected an = c.getAnnotation(Reflected.class);
		if (an != null) {
			List<String> names;
			if (an.value().length > 0)
				names = Arrays.asList(an.value());
			else
				names = Arrays.asList(c.getSimpleName());
			for(String n : names) {
				if (!namePattern.matcher(n).matches())
					throw new RuntimeException("invalid reflected name for "+c.getCanonicalName()+" has invalid reflected name "+an.value());
				classmap.put(n, c);
			}
		} else if (Reflectable.class.isAssignableFrom(c)) {
		} else {
			throw new RuntimeException("can't register unreflectable class "+c.getCanonicalName());
		}
		
		for(Field f : c.getFields()) {
			an = f.getAnnotation(Reflected.class);
			if (an == null) continue;
			

			List<String> names;
			if (an.value().length > 0)
				names = Arrays.asList(an.value());
			else
				names = Arrays.asList(c.getSimpleName());
			for(String n : names) {
				if (!namePattern.matcher(n).matches())
					throw new RuntimeException("invalid reflected name for "+c.getCanonicalName()+" has invalid reflected name "+an.value());
			}
		}
	}*/
	
	/**
	 * Adds a new MagicMirror to the list of mirrors to check.  Mirrors are checked in LIFO order.
	 * @param m mirror to add
	 */
	public static void registerMirror(MagicMirror<?> m) {
		magicMirrors.add(0, m);
	}

	public static Class<?> nameToClass(String sval) {
		return NamedTypes.types.get(sval);
	}
	
	@SuppressWarnings("unchecked")
	public final static <T> void serializeValue(T v, Serializer o) throws IOException {
		if (v == null) {
			o.write("null".getBytes());
			return;
		}
		
		if(v instanceof ReflectAware)
			((ReflectAware)v).willSerialize();
		
		Class<?> cls = v.getClass();
		
		Reflected an = v.getClass().getAnnotation(Reflected.class);
		if (an != null) {
			Integer objid = o.getObjID(v);
			if (objid != null) {
				o.write(("%"+objid.toString()).getBytes());
				return;
			} else {
				objid = o.generateObjID(v);
				o.write(("#"+an.value()[0]+" @"+objid+ " ").getBytes());
			}
		}
		
		try {
			for(MagicMirror<?> m : magicMirrors) {
				if (m.canReflect(cls)) {
					((MagicMirror<T>)m).serialize(v, o);
					return;
				}
			}
			throw new REThrow("Attempt to load unsupported type "+cls.getCanonicalName());
		}
		catch (IOException e) {throw e;}
		catch (Exception e) {throw new REThrow(e);}
	}
	
	@SuppressWarnings("unchecked")
	public final static <T> void unserializeValue(Type t, Deserializer scanner, T autoValue, Property<T> prop) throws SyntaxError, IOException {
		Class<?> cls;
		if (t instanceof Class)
			cls = (Class<?>) t;
		else
			cls = (Class<?>) ((ParameterizedType)t).getRawType();
		
		
		try {
			switch(scanner.nextToken()) {
			case '#': // reflected class
				if (scanner.nextToken() != Deserializer.TT_WORD)
					throw new SyntaxError("Expecting class name");
				Class<?> realclass = nameToClass(scanner.sval);
				if (realclass == null)
					throw new SyntaxError("Unknown class \""+scanner.sval+"\"");
				if (!cls.isAssignableFrom(realclass))
					throw new SyntaxError(realclass.getCanonicalName()+" does not extend/implement "+cls.getCanonicalName());
					
				cls = realclass;
				if (scanner.nextToken() != '@')
					throw new SyntaxError("Expecting \"@\"");
				if (scanner.nextToken() != Deserializer.TT_NUMBER)
					throw new SyntaxError("Expecting object index");
				int index = (int) scanner.nval;
				unserializeValue(cls, scanner, autoValue, prop);
				scanner.loadedObjects.put(index, prop.get());
				return;
			case '%': // repeated object
				if (scanner.nextToken() != Deserializer.TT_NUMBER)
					throw new SyntaxError("Expecting object index");
				if (!scanner.loadedObjects.containsKey((int)scanner.nval))
					throw new SyntaxError("Unknown object index");
				prop.set((T) scanner.loadedObjects.get((int)scanner.nval), false);
				return;
			case Deserializer.TT_WORD:
				if (scanner.sval.equals("null")){
					prop.set(null, false);
					return;
				}
			default:
				scanner.pushBack();
			}
			for(MagicMirror<?> m : magicMirrors) {
				if (m.canReflect(t)) {
					((MagicMirror<T>)m).deserialize(t, scanner, autoValue, prop);
					if(prop.get() instanceof ReflectAware)
						((ReflectAware)prop.get()).didDeserialize();
					return;
				}
			}
			throw new REThrow("Attempt to load unsupported type "+cls.getCanonicalName());
		}
		catch (SyntaxError e) {throw e;}
		catch (IOException e) {throw e;}
		catch (Exception e) {throw new REThrow(e);}
	}
	
	public static <T> T load(Class<T> type, Reader source, Object arg) throws IOException, SyntaxError {
		Deserializer in =  new Deserializer(source);
		AutoProperty<Object> prop = new AutoProperty<Object>(null, type, "value");
		try {
			unserializeValue(type, in, arg, prop);
			return type.cast(prop.get());
		} catch (SyntaxError e) {
			throw new SyntaxError(in.lineno()+": ", e);
		}
	}
	
	public static <T> T load(Class<T> type, File source) throws IOException, SyntaxError {
		FileReader in = new FileReader(source);
		try {
			return load(type, in, source);
		} catch (SyntaxError e) {
			throw new SyntaxError(source.getCanonicalPath()+":", e);
		} finally {
			in.close();
		}
	}
	
	public static void store(Object obj, OutputStream target) throws IOException {
		serializeValue(obj, new Serializer(target));
	}
	
	public static void store(Object obj, File target) throws IOException {
		FileOutputStream out = new FileOutputStream(target);
		try {
			store(obj, out);
		} finally {
			out.close();
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Editor<T> buildEditor(Property<? extends T> prop) {
		Class<?> cls = Utils.typeClass(prop.getType());
		if (prop.get() != null && cls.isAssignableFrom(prop.get().getClass()))
			cls = prop.get().getClass();
		
		if (prop.get() instanceof EditorAware) {
			((EditorAware)prop.get()).willEdit();
		}
		
		// first, try the field for a custom editor class:
		try { 
			GUIInfo an = prop.getAnnotation(GUIInfo.class);
			if (null != an) {
				Class<? extends Editor<? super T>> ed = (Class<? extends Editor<T>>) an.editor();
				if (!Editor.class.equals(ed)) {
					return (Editor<T>) Mirror.getConstructor(ed, Property.class).newInstance(prop);
				}
			}
		} catch (Exception e) {
			throw new REThrow(e);
		}
		
		// failing that, try the class itself:
		try { 
			GUIInfo an = cls.getAnnotation(GUIInfo.class);
			if (null != an) {
				Class<? extends Editor> ed = an.editor();
				if (!Editor.class.equals(ed)) {
					return Mirror.getConstructor(ed, Property.class).newInstance(prop);
				}
			}
		} catch (Exception e) {
			throw new REThrow(e);
		}
		
		// nope, none of that worked, search for a mirror that can reflect it
		for(MagicMirror m : magicMirrors) {
			if (m.canReflect(prop.getType())) {
				return m.buildEditor(prop);
			}
		}
		throw new REThrow("Attempt to edit unsupported type "+cls.getCanonicalName());
	}

	@SuppressWarnings("unchecked")
	public static Editor<?> buildEditor(Object o, String name) {
		return buildEditor(new AutoProperty(o, o.getClass(), name));
	}
	
	public static boolean isStrongType(Type t) {
		for(MagicMirror<?> m : magicMirrors) {
			if (m.canReflect(t)) {
				return m.isStrongClass(t);
			}
		}
		return true;
	//	throw new REThrow("Attempt to examine unsupported type "+ClassUtils.typeClass(t).getCanonicalName());
	}
	
	public static String getDisplayName(Type t) {
		if(t == null) return "null";
		Class<?> cls = Utils.typeClass(t);
		GUIInfo info = cls.getAnnotation(GUIInfo.class);
		if (info != null)
			if (info.name().length() > 0)
				return info.name();
		Reflected an = cls.getAnnotation(Reflected.class);
		if (an != null)
			if (an.value().length > 0)
				return an.value()[0];
		return cls.getSimpleName();
	}
	
	public static String getDisplayName(Field f) {
		if(f == null) return "!!null!!";
		GUIInfo info = f.getAnnotation(GUIInfo.class);
		if (info != null)
			if (info.name().length() > 0)
				return info.name();
		Reflected an = f.getAnnotation(Reflected.class);
		if (an != null)
			if (an.value().length > 0)
				return an.value()[0];
		return f.getName();
	}
	
	public static GUIInfo getInnerGUIInfo(InnerGUIInfo a, int path) {
		if(a == null) return null;
		for(final InnerGUIInfo.I data : a.value())
			if (data.path().length == 1 && data.path()[0] == path)
				return data.info();
		return null;
	}
	
	public static InnerGUIInfo getSubInnerGUIInfo(InnerGUIInfo a, int path) {
		if(a == null) return null;
		
		final ArrayList<InnerGUIInfo.I> datas = new ArrayList<InnerGUIInfo.I>();
		
		for(final InnerGUIInfo.I data : a.value()) {
			if (data.path().length > 0 && data.path()[0] == path) {
				datas.add(new InnerGUIInfo.I() {
					final int[] path = Arrays.copyOfRange(data.path(), 1, data.path().length);
					public int[] path() {return path;}
					public GUIInfo info() {return data.info();}
					public Class<? extends Annotation> annotationType()
						{return InnerGUIInfo.I.class;}
				});
			}
		}
		
		return new InnerGUIInfo() {
			InnerGUIInfo.I[] data = datas.toArray(new InnerGUIInfo.I[datas.size()]);
			public InnerGUIInfo.I[] value() {return data;}
			public Class<? extends Annotation> annotationType() {
				return InnerGUIInfo.class;
			}
		};
	}
	
	/**
	 * Intelligently instantiate an object to populate a property.  In order:
	 * <ol>
	 * <li> checks for a GUIInfo.constructor annotation and calls that
	 * <li> searches for a single-argument constructor compatible with the passed autoValue
	 * <li> searches for mirror capable of constructing the type (then probably calling dumbInstantiate)
	 * </ol>
	 * @param <T> type of target
	 * @param prop target into which to instantiate
	 * @param t override the type of prop
	 * @param autoValue construction hint, may be used as type hint
	 * @param log should the event be recorded to the undo log
	 * @see #dumbInstantiate(Property, Type, boolean, Object)
	 */
	/**
	 * @param <T>
	 * @param prop
	 * @param t
	 * @param autoValue
	 * @param log
	 */
	@SuppressWarnings("unchecked")
	public static <T> void instantiateInto(Property<T> prop, Type t, Object autoValue, boolean log) {
		if (!(prop instanceof TempProperty) && prop.get() != null) return;
		
		Class<? extends T> c = (Class<? extends T>) Utils.typeClass(t);

		// first check the property:
		if (attemptConstruction(prop.getAnnotation(GUIInfo.class), prop, t, log)) return;

		// then check the type:
		if (attemptConstruction(Utils.typeClass(t).getAnnotation(GUIInfo.class), prop, t, log)) return;
		
		if (autoValue != null) {
			for(Constructor<?> ctor : c.getConstructors()) {
				if (ctor.getParameterTypes().length != 1) continue;
				if (ctor.getParameterTypes()[0].isAssignableFrom(autoValue.getClass())) {
					try {
						prop.set((T) ctor.newInstance(autoValue), log);
						return;
					} catch (InvocationTargetException ite) {
						Throwable e = ite.getTargetException();
						e.printStackTrace();
						throw new REThrow(e);
					} catch (Exception e) {
					}
					break;
				}
			}
		}
		
		try {
			for(MagicMirror<?> m : magicMirrors) {
				if (m.canReflect(t)) {
					((MagicMirror<T>)m).instantiateInto(prop, t, log);
					return;
				}
			}
			throw new REThrow("instantiate unknown class " + t.toString());
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}
	
	/**
	 * Fixed-function instantiation.  Attempts to use a 0- or 1-argument constructor.  If the
	 * type is a member type, first attempts instantiation with the property's {@link Property#context() context}</code>,
	 * and then <code>arg</code>.  In the first case above or the non-member-class case, if <code>arg</code> is null
	 * then only construction through a single-argument constructor will be attempted
	 * @param <T>
	 * @param target destination of the instantiated value
	 * @param t type to instantiate.  must match &lt;? extends T>
	 * @param log record to undo log
	 * @param arg argument to constructor, or outer instance
	 * @return the instantiated object
	 */
	@SuppressWarnings("unchecked")
	public static <T> void dumbInstantiateInto(Property<T> target, Type t, boolean log, Object arg) {
		if (t == null) t = target.getType();
		Class<? extends T> c = (Class<? extends T>) Utils.typeClass(t);
		T out;
		
		if (c.isArray()) {
			out = (T) Array.newInstance(c.getComponentType(), (Integer)arg);
			target.set(out, log);
			return;
		}
		
		try {
			if (c.isMemberClass() && (c.getModifiers()&Modifier.STATIC) == 0) {
				if (target.context()!=null && c.getEnclosingClass().isAssignableFrom(target.context().getClass())) {
					if (arg != null) {
						out = getConstructor(c, c.getEnclosingClass(), arg.getClass()).newInstance(target.context(), arg);
					} else {
						out = getConstructor(c, c.getEnclosingClass()).newInstance(target.context());
					}
				} else {
					if (arg != null && c.getEnclosingClass().isAssignableFrom(arg.getClass())) {
						out = getConstructor(c, c.getEnclosingClass()).newInstance(arg);
					}
					throw new REThrow("missing context");
				}
			} else {
				if (arg != null)
					out = (T) Mirror.getConstructor(Utils.typeClass(t), arg.getClass()).newInstance(arg);
				else
					out = (T) Mirror.getConstructor(Utils.typeClass(t)).newInstance();
			}
			target.set(out, log);
		} catch(Exception e) {
			throw new REThrow(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> boolean attemptConstruction(GUIInfo info, Property<T> prop,
			Type t, boolean log) {
		if (info == null) return false;
		if (info.constructor() != DefaultConstructor.class) try {
			Class<? extends DefaultConstructor> c = info.constructor();
			if (c.isMemberClass() && (c.getModifiers()&Modifier.STATIC) == 0) {
				if (c.getEnclosingClass().isAssignableFrom(prop.context().getClass())) {
					Mirror.getConstructor(c, c.getEnclosingClass())
						.newInstance(prop.context()).instantiateInto(prop, t, log);
					return true;
				} else {
					System.err.printf("Can't construct member DefaultConstructor: contenxt (%s) is not parent (%s)\n",
							prop.context().getClass(), c.getEnclosingClass());
				}
			} else {
				Mirror.getConstructor(c).newInstance().instantiateInto(prop, t, log);
				return true;
			}
		} catch (Exception e) {
			System.err.println("Error calling constructor: "+e);
		}
		if (info.constructWithContext()) try {
			dumbInstantiateInto(prop, t, log, prop.context());
			return true;
		} catch (Exception e) {
			System.err.println("Error calling constructor: "+e);
		}
		return false;
	}

	/**
	 * Convenience wrapper for {@link #instantiateInto(Property,Type,Object,boolean)}.
	 * The type and autoValue arguments are taken from the supplied {@link Property}.
	 * @param <T> type of target
	 * @param prop target into which to instantiate
	 * @param log should the event be recorded to the undo log
	 */
	public static <T> void instantiateInto(Property<T> prop, boolean log) {
		instantiateInto(prop, prop.getType(), prop.get(), log);
	}
	
	static public Field getField(Type t, String name) {
		if (t==null) return null;
		Class<?> c = Utils.typeClass(t);
		Field out = null;
		try {
			out = c.getDeclaredField(name);
		} catch (Exception e) {
		}
		if (out == null)
			out = getField(c.getSuperclass(), name);
		out.setAccessible(true);
		return out;
	}
	
	static public Method getMethod(Type t, String name, Class<?>... args) {
		if (t==null) return null;
		Class<?> c = Utils.typeClass(t);
		Method out = null;
		try {
			out = c.getDeclaredMethod(name, args);
		} catch (Exception e) {
		}
		if (out == null)
			for(Class<?> i : c.getInterfaces())
				if ((out = getMethod(i, name, args)) != null)
					break;
		if (out == null)
			out = getMethod(c.getSuperclass(), name, args);
		out.setAccessible(true);
		return out;
	}
	
	@SuppressWarnings("unchecked")
	static public <T> Constructor<T> getConstructor(Class<T> c, Class<?>... args) {
		if (c==null) return null;
		try {
			for(Constructor ctor : c.getDeclaredConstructors()) {
				Class[] params = ctor.getParameterTypes();
				if (params.length != args.length) continue;
				boolean match = true;
				for(int i=0 ; i<params.length ; i++)
					if (!params[i].isAssignableFrom(args[i])) {
						match = false;
						break;
					}
				if(match) {
					ctor.setAccessible(true);
					return ctor;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	static public Field[] getFieldsWithAnnotation(Type t, Class<? extends Annotation> anno) {
		if (t==null) return new Field[0];
		ArrayList<Field> out = new ArrayList<Field>();
		Class<?> c = Utils.typeClass(t);
		for (Field f : c.getDeclaredFields()) {
			Annotation r = f.getAnnotation(anno);
			if (r == null) continue;
			f.setAccessible(true);
			out.add(f);
		}
		out.addAll(Arrays.asList(getReflectedFields(c.getSuperclass())));
		return out.toArray(new Field[out.size()]);
	}
	
	static public Field[] getReflectedFields(Type t) {
		return getFieldsWithAnnotation(t, Reflected.class);
	}

	public static Annotation[] getInnerAnnotations(Property<?> prop, int path) {
		ArrayList<Annotation> ans = new ArrayList<Annotation>();
		ans.add(getInnerGUIInfo(prop.getAnnotation(InnerGUIInfo.class), path));
		ans.add(getSubInnerGUIInfo(prop.getAnnotation(InnerGUIInfo.class), path));
		ans.removeAll(Arrays.asList((Object)null));
		return ans.toArray(new Annotation[ans.size()]);
	}

	public static Object getContainingInstance(Object o) {
		Class<?> inner = o.getClass();
		if(!inner.isMemberClass() || (inner.getModifiers() & Modifier.STATIC) != 0) return null;
		Class<?> outer = inner.getEnclosingClass();
		for(Field f : inner.getDeclaredFields()) {
			String name = f.getName();
			if (name.startsWith("this$") && outer.isAssignableFrom(f.getType())) {
				f.setAccessible(true);
				try {
					return f.get(o);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("couldn't get containing instance");
					return null;
				}
			}
		}
		System.err.println("couldn't get containing instance");
		return null;
	}
}
