package reflect;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import util.Utils;
import util.REThrow;

class ObjectMirror implements MagicMirror<Object> {

	public boolean canReflect(Type type) {
		if (type instanceof Class && Object.class.isAssignableFrom((Class<?>) type)) {
			return true;
		} else {
			return canReflect(((ParameterizedType)type).getRawType());
		} 
	}

	public void deserialize(Type gt, Deserializer scanner, Object autoValue, Property<Object> target) throws IOException, SyntaxError {
		Object out = null;

		Mirror.instantiateInto(target, gt, autoValue, false);
		
		out = target.get();

		HashMap<String, Field> fields = new HashMap<String, Field>();
		for(Field f : Mirror.getReflectedFields(out.getClass())) {
			Reflected an = f.getAnnotation(Reflected.class);
			if (an==null) continue;
			if (an.value().length > 0) {
				for(String n : an.value())
					fields.put(n, f);
			} else {
				fields.put(f.getName(), f);
			}
		}

		scanner.expect('{');
		
		while (scanner.nextToken() == '$') {
			if (scanner.nextToken() != StreamTokenizer.TT_WORD)
				throw new SyntaxError("expecting field name");
			String fname = scanner.sval;
			Field f = fields.get(fname);
			scanner.expect('=');
			if (f == null) {
				System.out.println("Attempted to load to unknown field: "+fname);
				int depth = 0;
				while(depth >= 0) {
					switch(scanner.nextToken()) {
					case '{':
						depth++;
						break;
					case '}':
						depth--;
						break;
					case ';':
						if (depth == 0) {
							depth = -1;
							scanner.pushBack();
						}
						break;
					}
				}
			} else {
				FieldProperty<Object> prop = new FieldProperty<Object>(out, f);
				Mirror.unserializeValue(f.getGenericType(), scanner, prop.get(), prop);
			}
			scanner.expect(';');
		}
		scanner.pushBack();
		scanner.expect('}');
	}

	public void serialize(Object v, Serializer o) throws IOException {
		o.write(" {\n".getBytes());
		o.indentin();
		
		for(Field f : Mirror.getReflectedFields(v.getClass())) {
			Reflected an = f.getAnnotation(Reflected.class);
			if (an == null) continue;
			String name = f.getName();
			if (an.value().length > 0) name = an.value()[0];
			
			o.indent();
			o.write(String.format("$%s=", name).getBytes());
			try {
				Mirror.serializeValue(f.get(v), o);
			} catch (Exception e) {
				throw new REThrow(e);
			}
			o.write(";\n".getBytes());
		}
		o.indentout();
		o.indent();
		o.write("}\n".getBytes());
	}
	
	@SuppressWarnings("serial")
	static class MyEditor extends Editor<Object> implements ActionListener {
		Object prevValue;
		JComboBox classmenu;
		ArrayList<Class<?>> classlist = new ArrayList<Class<?>>();
		ArrayList<String> classtips = new ArrayList<String>();
		Box contentBox;
		int headerHeight;

		public MyEditor(Property<Object> prop) {
			super(prop, BoxLayout.Y_AXIS);
			this.setBorder(BorderFactory.createTitledBorder(label()));
			setPreferredSize(new Dimension(0, 1));
			
			boolean nullOK = false, classLocked = false;
			GUIInfo finfo = prop.getAnnotation(GUIInfo.class);
			if (finfo != null) for(GUIInfo.GUIInfoFlags f : finfo.flags()) {
				if (f == GUIInfo.GUIInfoFlags.classLocked)
					classLocked = true;
				if (f == GUIInfo.GUIInfoFlags.nullOK)
					nullOK = true;
			}

			if (nullOK) {
				classlist.add(null);
				classtips.add("");
			}

			Class<?> fieldClass = Utils.typeClass(prop.getType());
			Reflected typean = fieldClass.getAnnotation(Reflected.class);
			prevValue = prop.get();
			if (typean != null && !classLocked) {
				for(Map.Entry<String, Class<?>> ent :Mirror.getClassMap().entrySet())
					if (fieldClass.isAssignableFrom(ent.getValue()) &&
							!((ent.getValue().getModifiers() & Modifier.ABSTRACT) != 0))
						classlist.add(ent.getValue());
			} else if (prevValue != null) {
				classlist.add(prevValue.getClass());
			} else {
				classlist.add(fieldClass);
			}
			if (classlist.size() > 1) {
				Box menubox = new Box(BoxLayout.X_AXIS);
				menubox.add(new JLabel("Class:"));
				String menuitems[] = new String[classlist.size()];
				int selectedIndex=0;
				for(int i=0 ; i<menuitems.length ; i++) {
					Class<?> c = classlist.get(i), prevclass = null;
					if (prevValue != null) prevclass = prevValue.getClass();
					String tip = "";
					String name = Mirror.getDisplayName(c);
					if (c == null) {
						if (prevValue == null) 
							selectedIndex = i;
						tip = "null value";
					} else {
						if (c.equals(prevclass))
							selectedIndex = i;
						GUIInfo an = c.getAnnotation(GUIInfo.class);
						if (an != null)
							tip = an.tip();
					}
					menuitems[i] = name;
					classtips.add(tip);
				}

				classmenu = new JComboBox(menuitems);
				classmenu.setSelectedIndex(selectedIndex);
				classmenu.setRenderer(new BasicComboBoxRenderer() {
					@Override
					public Component getListCellRendererComponent(JList list,
							Object value, int index, boolean isSelected,
							boolean cellHasFocus) {
						Component out = super.getListCellRendererComponent(list, value, index, isSelected,
								cellHasFocus);
						if (index >=0
						 && out instanceof JComponent
						 && classtips.get(index).length() > 0)
							((JComponent)out).setToolTipText(classtips.get(index));
						return out;
					}
				});
				
				classmenu.addActionListener(this);
				classmenu.setMaximumSize(new Dimension(1000, classmenu.getPreferredSize().height));
				headerHeight = classmenu.getPreferredSize().height;
				menubox.add(classmenu);
				add(menubox);
				JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
				sep.setMaximumSize(new Dimension(1000, sep.getPreferredSize().height));
				headerHeight += sep.getPreferredSize().height;
				add(sep);
				contentBox = new Box(BoxLayout.Y_AXIS);
				add(contentBox);
			} else {
				contentBox = this;
				headerHeight = 0;
			}
			populate();
		}

		public void actionPerformed(ActionEvent e) {
			// class menu selected
			Class<?> newclass = classlist.get(classmenu.getSelectedIndex());
			if (newclass == null) {
				if (prevValue == null) return;
				UndoLog.staticUndoLog().begin("Clear "+label());
				prop.set(null, true);
				UndoLog.staticUndoLog().end();
			} else {
				if (prevValue != null && newclass.equals(prevValue.getClass())) return;
				try {
					UndoLog.staticUndoLog().begin("Change "+label());
					prop.set(null, true);
					Mirror.instantiateInto(prop, newclass, null, true);
					UndoLog.staticUndoLog().end();
					setDirty();
				} catch (Exception ex) {
					throw new REThrow(ex);
				}
			}
			externallyUpdated();
		}
		
		protected void populate() {
			contentBox.removeAll();
			
			Field[] fields = Mirror.getReflectedFields(prop.get().getClass());
			Arrays.sort(fields, new Comparator<Field>(){
				public int compare(Field arg0, Field arg1) {
					int order0 = 1000;
					int order1 = 1000;
					GUIInfo an;
					if(null != (an = arg0.getAnnotation(GUIInfo.class))) order0 = an.order();
					if(null != (an = arg1.getAnnotation(GUIInfo.class))) order1 = an.order();
					return order0-order1;
				}
			});
			int height = 0;
			
			if (prop.get() != null) for(Field f : fields) {
				Reflected an = f.getAnnotation(Reflected.class);
				if (an==null) continue;
				
				GUIInfo info = f.getAnnotation(GUIInfo.class);
				if (info != null)
					if (info.hide()) continue;
				
				Editor<?> ed = Mirror.buildEditor(new FieldProperty<Object>(prop.get(), f));
				height += ed.getMinimumSize().height;
				contentBox.add(ed);
			}
			contentBox.setMinimumSize(new Dimension(1, height));
			contentBox.setPreferredSize(new Dimension(0, height));
			int insets = getInsets().top + getInsets().bottom;
			setMinimumSize(new Dimension(1, height+headerHeight+insets));
			setPreferredSize(new Dimension(0, height+headerHeight+insets));
			revalidate();
		}
		
		@Override
		public void externallyUpdated() {
			populate();
			boolean dirty = false;
			if (prevValue == null) {
				if (prop.get() != null) {
					classmenu.setSelectedIndex(classlist.indexOf(prop.get().getClass()));
					dirty = true;
				}
			} else if (prop.get() == null) {
				if (prevValue != null) {
					classmenu.setSelectedIndex(0);
					dirty = true;
				}
			} else {
				if (prevValue != prop.get())
					if (!classlist.get(classmenu.getSelectedIndex()).equals(prop.get().getClass())) {
						classmenu.setSelectedIndex(classlist.indexOf(prop.get().getClass()));
						dirty = true;
					}
			}
			prevValue = prop.get();
			if(dirty)
				this.revalidate();
			else
				this.repaint();
			super.externallyUpdated();
		}
	}

	public Editor<Object> buildEditor(Property<Object> prop) {
		return new MyEditor(prop);
	}

	public void instantiateInto(Property<Object> prop, Type t, boolean log) {
		Mirror.dumbInstantiateInto(prop, t, log, null);
	}

	public boolean isStrongClass(Type t) {
		return true;
	}
}
