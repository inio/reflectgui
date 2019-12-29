package reflect;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import util.REThrow;

abstract public class Tuple<T extends Tuple<?>> implements Reflectable<T> {
	public void serialize(Serializer o) throws IOException {
		boolean first = true;
		TreeMap<Integer, Field> valuemap = new TreeMap<Integer, Field>();
		for(Field f : Mirror.getFieldsWithAnnotation(getClass(), TupleField.class)) {
			TupleField an = f.getAnnotation(TupleField.class);
			if (an == null) continue;
			try {
				valuemap.put(an.value(), f);
			} catch (NumberFormatException e) {
				throw new REThrow("couldn't parse tuple index for "+this.getClass().getCanonicalName()+"."+f.getName());
			}
		}
		o.write("{".getBytes());
		for(Field f : valuemap.values()) {
			if(!first) o.write(" ".getBytes());
			try {
				Mirror.serializeValue(f.get(this), o);
			} catch (IllegalAccessException e) {
				throw new REThrow(e);
			}
			first = false;
		}
		o.indent();
		o.write("}".getBytes());
	}
	
	public void deserialize(Deserializer scanner, Object context) throws IOException, SyntaxError {
		TreeMap<Integer, Field> valuemap = new TreeMap<Integer, Field>();
		for(Field f : Mirror.getFieldsWithAnnotation(getClass(), TupleField.class)) {
			TupleField an = f.getAnnotation(TupleField.class);
			if (an == null) continue;
			try {
				valuemap.put(an.value(), f);
			} catch (NumberFormatException e) {
				throw new SyntaxError("couldn't parse tuple index for "+this.getClass().getCanonicalName()+"."+f.getName());
			}
		}
		
		if (scanner.nextToken() != '{')
			throw new SyntaxError("Tuple must start with \"{\"");
		for(Field f : valuemap.values()) {
			try {
				FieldProperty<Object> prop = new FieldProperty<Object>(this, f);
				Mirror.unserializeValue(f.getGenericType(), scanner, f.get(this), prop);
			} catch (IllegalAccessException e) {
				throw new REThrow(e);
			}
		}
		if (scanner.nextToken() != '}')
			throw new SyntaxError("Too many values in tuple");
	}
	

	@SuppressWarnings("serial")
	static class MyEditor<T> extends Editor<T> {
		Object prevValue;

		public MyEditor(Property<T> prop) {
			super(prop, BoxLayout.Y_AXIS);
			this.setBorder(BorderFactory.createTitledBorder(label()));
			
			prevValue = prop.get();
			
			populate();
		}
		
		protected void populate() {
			removeAll();
			TreeMap<Integer, Field> fields = new TreeMap<Integer, Field>();
			
			if (prop.get() == null) 
				Mirror.instantiateInto(prop, true);
			
			for(Field f : Mirror.getFieldsWithAnnotation(prop.get().getClass(), TupleField.class)) {
				TupleField tup = f.getAnnotation(TupleField.class);
				if (tup == null) continue;
				fields.put(tup.value(), f);
			}
			
			for(Field f : fields.values()) {
				GUIInfo info = f.getAnnotation(GUIInfo.class);
				if (info != null)
					if (info.hide()) continue;
				
				add(Mirror.buildEditor(new FieldProperty<Object>(prop.get(), f)));
			}
			revalidate();
		}
		
		@Override
		public void externallyUpdated() {
			populate();
			super.externallyUpdated();
		}
	}
	
	public Editor<T> buildEditor(Property<T> prop) {
		return new MyEditor<T>(prop);
	}
}
