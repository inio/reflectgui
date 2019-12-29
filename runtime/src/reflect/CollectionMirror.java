/*
 * Copyright 2009 Ian Rickard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reflect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import util.Utils;
import util.IconCache;
import util.REThrow;

public class CollectionMirror implements MagicMirror<Object> {

	public boolean canReflect(Type type) {
		if (type instanceof Class) {
			if (((Class<?>) type).isArray()) return true;
			return Collection.class.isAssignableFrom((Class<?>) type);
		} else {
			return canReflect(((ParameterizedType)type).getRawType());
		}
	}
	
	@SuppressWarnings("unchecked")
	public void deserialize(Type gt, Deserializer scanner, Object autoValue, Property<Object> target) 
			throws IOException, SyntaxError {
		Class<?> t;
		if(gt instanceof Class)
			t = (Class<?>)gt;
		else
			t = (Class<?>)((ParameterizedType)gt).getRawType();
		
		if (Mirror.braceLists) scanner.expect('{');
		scanner.expect('[');
		scanner.expectWord("size");
		scanner.expect('=');
		if (scanner.nextToken() != StreamTokenizer.TT_NUMBER)
			throw new SyntaxError("expecting number in array size");
		int len = (int)scanner.nval;
		scanner.expect(']');
		
		if (t.isArray()) {
			boolean strong = Mirror.isStrongType(t.getComponentType());
			Mirror.dumbInstantiateInto(target, t, false, len);
		//	out = Array.newInstance(t.getComponentType(), len);
			for(int i=0 ; i<len ; i++) {
				if (!strong) scanner.expect('{');
				Property<?> prop = new ListElement(target.get(), i, t.getComponentType(),
						Mirror.getInnerAnnotations(target, 0), target.context());
				Mirror.unserializeValue(t.getComponentType(), scanner, null, prop);
				if (!strong) scanner.expect('}');
			}
			target.set(target.get(), false);
		} else try {
			Type element = ((ParameterizedType)gt).getActualTypeArguments()[0];
			boolean strong = Mirror.isStrongType(element);
			Mirror.instantiateInto(target, gt, autoValue, false);
			for(int i=0 ; i<len ; i++) {
				if (!strong)
					scanner.expect('{');
				Property<?> prop = new BagElement((Collection<? extends Object>) target.get(), element,
						Mirror.getInnerAnnotations(target, 0), target.context());
				Mirror.unserializeValue(element, scanner, null, prop);
				if (!strong) scanner.expect('}');
			}
			target.set(target.get(), false);
		} catch (SyntaxError e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new REThrow(e);
		}

		if (Mirror.braceLists) scanner.expect('}');
	}

	public void serialize(final Object v, Serializer o) throws IOException {
		Collection<?> elms;
		
		if (v.getClass().isArray()) {
			elms = new AbstractList<Object>() {
				@Override public Object get(int i) {return Array.get(v, i);}
				@Override public int size() {return Array.getLength(v);}
			};
		} else {
			elms = (Collection<?>) v;
		}

		if (Mirror.braceLists)
			o.write('{');
		o.write(String.format("[size=%d]", elms.size()).getBytes());
		o.indentin();
		for (Object val : elms) {
			o.indent();
			o.write(' ');
			boolean strong = Mirror.isStrongType(val.getClass());
			if (!strong) o.write('{');
			Mirror.serializeValue(val, o);
			if (!strong) o.write('}');
		}
		o.indentout();
		o.indent();
		if (Mirror.braceLists) {
			o.indent();
			o.write('}');
		}
	}
	
	static class ListElement extends UndoableAbstractProperty<Object> {
		Object list;
		int index;
		Object context;
		
		public ListElement(Object list, int index, Type type, Annotation[] ans, Object context) {
			super(type, "", ans);
			this.list = list;
			this.index = index;
			this.context = context;
		}
		
		@SuppressWarnings("unchecked")
		public Object get() {
			if (list instanceof List)
				return ((List)list).get(index);
			else
				return Array.get(list, index);
		}
		
		@SuppressWarnings("unchecked")
		public void set(Object val) {
			if (list instanceof List)
				((List)list).set(index, val);
			else
				Array.set(list, index, val);
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof ListElement)) return false;
			ListElement le = (ListElement) obj;
			return le.list==list && le.index == index;
		}

		public Object context() {
			return context;
		}
	}

	static class BagElement extends UndoableAbstractProperty<Object> {
		Collection<? extends Object> bag;
		Object value, context;
		
		public BagElement(Collection<? extends Object> bag, Object value, Type type, Annotation[] ans, Object context) {
			super(type, "", ans);
			this.bag = bag;
			this.value = value;
			this.context = context;
		}
		
		public BagElement(Collection<? extends Object> bag, Type type, Annotation[] ans, Object context) {
			super(type, "", ans);
			this.bag = bag;
			this.value = this;
			this.context = context;
		}

		public Object get() {
			return (value==this)?null:value;
		}
		
		@SuppressWarnings("unchecked")
		public void set(Object val) {
			if (value != this)
				bag.remove(value);
			((Collection<Object>)bag).add(val);
			value = val;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof BagElement)) return false;
			BagElement be = (BagElement) obj;
			return be.bag==bag && be.value.equals(value);
		}

		public Object context() {
			return context;
		}
	}
	
	@SuppressWarnings("serial")
	static class MyEditor extends Editor<Object> implements ActionListener  {
		
		Member prevCheck = null;
		
		class Member extends Box implements ActionListener {
			Editor<Object> ed;
			JCheckBox checkbox;
			Property<Object> prop;
			boolean wasSelected;
			
			public Member(Property<Object> prop) {
				super(BoxLayout.X_AXIS);
				this.prop = prop;
				checkbox = new JCheckBox();
				checkbox.addActionListener(this);
				checkbox.setAlignmentY(0);
				add(checkbox);
				ed = Mirror.buildEditor(prop);
				ed.setAlignmentY(0);
				add(ed);
			}
			
			public void actionPerformed(ActionEvent e) {
				if (checkbox.isSelected() == wasSelected) return;
				wasSelected = checkbox.isSelected();
				if (prevCheck != null && e!= null && (e.getModifiers() & ActionEvent.SHIFT_MASK)!=0) {
					int a = contents.indexOf(this);
					int b = contents.indexOf(prevCheck);
					if (a>b) {int tmp = b;b=a;a=tmp;}
					for(int i=a ; i<=b ; i++) {
						contents.get(i).checkbox.setSelected(wasSelected);
						contents.get(i).actionPerformed(null);
					}
					return;
				}
				if (checkbox.isSelected())
					numSelected++;
				else
					numSelected--;
				deleteButton.setEnabled(numSelected>0);
				if (ordered) {
					upButton.setEnabled(numSelected>0);
					downButton.setEnabled(numSelected>0);
				}
				prevCheck = this;
			}
			
			boolean isSelected() {
				return checkbox.isSelected();
			}
			
			void setSelected(boolean b) {
				checkbox.setSelected(b);
				actionPerformed(null);
			}
		}

		Object oldval;
		Type memberType;
		Annotation[] memberans;
		boolean ordered;
		boolean nullOK;
		int numSelected;
		
		ArrayList<Member> contents = new ArrayList<Member>();
		
		Box contentBox;
		
		JButton deleteButton;
		JButton upButton;
		JButton downButton;
	//	GUIInfo memberInfo;
		
		protected MyEditor(Property<Object> prop) {
			super(prop, BoxLayout.Y_AXIS);
			this.setBorder(BorderFactory.createTitledBorder(label()));
			
			contentBox = new Box(BoxLayout.Y_AXIS);
			add(contentBox);
			
			Box buttonBox = new Box(BoxLayout.X_AXIS);
			JButton button;
			
			nullOK = false;
			
			GUIInfo info = prop.getAnnotation(GUIInfo.class);
			if (info != null)
				for(GUIInfo.GUIInfoFlags f : info.flags())
					if (f == GUIInfo.GUIInfoFlags.nullOK)
						nullOK = true;
			
			if (prop.getType() instanceof Class && ((Class<?>)prop.getType()).isArray()) {
				// box incase it's a primitive array
				memberType = Utils.box(((Class<?>)prop.getType()).getComponentType());
				ordered = true;
			} else if (prop.getType() instanceof GenericArrayType) {
				// generic arrays are never primitive-based
				memberType = ((GenericArrayType)prop.getType()).getGenericComponentType();
				ordered = true;
			} else {
				memberType = ((ParameterizedType)prop.getType()).getActualTypeArguments()[0];
				ordered = List.class.isAssignableFrom(Utils.typeClass(prop.getType()));
			}
			
			memberans = Mirror.getInnerAnnotations(prop, 0);
			
			button = new JButton(IconCache.get("plus"));
			button.setActionCommand("add");
			button.addActionListener(this);
			buttonBox.add(button);
			
			if (ordered) {
				upButton = new JButton(IconCache.get("up"));
				upButton.setActionCommand("up");
				upButton.addActionListener(this);
				buttonBox.add(upButton);
				
				downButton = new JButton(IconCache.get("down"));
				downButton.setActionCommand("down");
				downButton.addActionListener(this);
				buttonBox.add(downButton);
			}

			deleteButton = new JButton(IconCache.get("x"));
			deleteButton.setActionCommand("remove");
			deleteButton.addActionListener(this);
			buttonBox.add(deleteButton);
			add(buttonBox);
			
			reload();
		}
		
		void reload() {
			numSelected = 0;
			final Object v = prop.get();
			if (v == null) {
				contents = null;
			} else {
				Collection<?> elms;
				
				if (v.getClass().isArray()) {
					elms = new AbstractList<Object>() {
						@Override public Object get(int i) {return Array.get(v, i);}
						@Override public int size() {return Array.getLength(v);}
					};
				} else {
					elms = (Collection<?>) v;
				}

				contents = new ArrayList<Member>(elms.size());
				
				if (ordered) {
					for(int i=0 ; i<elms.size(); i++)
						contents.add(new Member(new ListElement(v, i, memberType, memberans, prop.context())));
				} else {
					for(Object e : elms)
						contents.add(new Member(new BagElement(elms, e, memberType, memberans, prop.context())));
				}
			}
			
			populate();
		}
		
		void populate() {
			contentBox.removeAll();
			if (contents != null) {
				for(Member m : contents)
					contentBox.add(m);
				if (contents.size() == 0)
					deleteButton.setEnabled(nullOK);
				deleteButton.setEnabled(numSelected>0);
				if (ordered) {
					upButton.setEnabled(numSelected>0);
					downButton.setEnabled(numSelected>0);
				}
			} else {
				contentBox.add(new JLabel("null"));
				deleteButton.setEnabled(false);
				if (ordered) {
					upButton.setEnabled(false);
					downButton.setEnabled(false);
				}
			}
			contentBox.revalidate();
		}
		
		void commit(String event) {
			TempProperty<Object> newprop = new TempProperty<Object>(prop);
			if (contents == null) {
				newprop.set(null, false);
			} else {
				if (Utils.typeClass(prop.getType()).isArray()) {
					newprop.set(Array.newInstance(
						Utils.typeClass(prop.getType()).getComponentType(),
						contents.size()
					), false);
					for(int i=0 ; i<contents.size() ; i++)
						Array.set(newprop.get(), i, contents.get(i).prop.get());
				} else if (Collection.class.isAssignableFrom(Utils.typeClass(prop.getType()))) {
					try {
						Mirror.instantiateInto(newprop, prop.getType(), null, false);
						Method addMethod = newprop.get().getClass().getMethod("add", Object.class);
						for(Member mem : contents)
							addMethod.invoke(newprop.get(), mem.prop.get());
					} catch (Exception e) {
						throw new REThrow(e);
					}
				} else {
					throw new REThrow("bad type");
				}
			}
			if (event != null) {
				UndoLog.staticUndoLog().begin(event);
				newprop.commit(true);
				UndoLog.staticUndoLog().end();
			} else {
				newprop.commit(false);
			}
			setDirty();
		}
		
		public void actionPerformed(ActionEvent act) {
			String cmd = act.getActionCommand();
			if (cmd.equals("add")) {
				if (contents == null) {
					contents = new ArrayList<Member>();
				} else {
					Property<Object> newprop = new AutoProperty<Object>(
							null, memberType, "", memberans, prop.context());
					Mirror.instantiateInto(newprop, false);
					
					contents.add(new Member(newprop));
				}
				commit("Add Item");
				reload();
			}
			
			if (cmd.equals("remove")) {
				int removed = 0;
				if (contents.size() == 0) {
					contents = null;
				} else {
					Iterator<Member> mi = contents.iterator();
					while(mi.hasNext()) {
						Member m = mi.next();
						if (m.isSelected()) {
							mi.remove();
							removed++;
						}
					}
				}
				commit((removed != 1)?"Remove Items":"Remove Item");
				reload();
			}
			
			if (cmd.equals("up")) {
				boolean solid=true;
				int count = 0;
				ArrayList<Integer> selected = new ArrayList<Integer>();
				for(int i=0 ; i<contents.size() ; i++) {
					if (contents.get(i).isSelected()) {
						count++;
						if (!solid) {
							selected.add(i-1);
							contents.add(i-1, contents.remove(i));
						} else {
							selected.add(i);
						}
					} else {
						solid = false;
					}
				}
				commit((count != 1)?"Shift Items Up":"Shift Item Up");
				reload();
				for(int i : selected)
					contents.get(i).setSelected(true);
			}
			
			if (cmd.equals("down")) {
				boolean solid=true;
				int count = 0;
				ArrayList<Integer> selected = new ArrayList<Integer>();
				for(int i=contents.size()-1 ; i>=0 ; i--) {
					if (contents.get(i).isSelected()) {
						count++;
						if (!solid) {
							selected.add(i+1);
							contents.add(i+1, contents.remove(i));
						} else {
							selected.add(i);
						}
					} else {
						solid = false;
					}
				}
				commit((count != 1)?"Shift Items Down":"Shift Item Down");
				reload();
				for(int i : selected)
					contents.get(i).setSelected(true);
			}
		}
		
		@Override
		public void externallyUpdated() {
			if (prop.get() != oldval && (prop.get() == null || !prop.get().equals(oldval))) {
				reload();
			}
			super.externallyUpdated();
		}
	}
	
	public Editor<Object> buildEditor(Property<Object> prop) {
		return new MyEditor(prop);
	}

	public boolean isStrongClass(Type t) {
		return false;
	}

	public void instantiateInto(Property<Object> prop, Type t, boolean log) {
		try {
			Mirror.dumbInstantiateInto(prop, t, log, null);
		} catch (Exception e) {
			if (Utils.typeClass(t).isAssignableFrom(ArrayList.class)) {
				Mirror.dumbInstantiateInto(prop, ArrayList.class, log, null);
				return;
			}
			throw new REThrow(e);
		} 
	}
}
