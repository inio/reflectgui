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
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import util.Utils;
import util.IconCache;
import util.REThrow;

public class MapMirror<K extends Object, V extends Object> implements MagicMirror<Map<K,V>> {

	public boolean canReflect(Type type) {
		if (type instanceof Class) {
			return Map.class.isAssignableFrom((Class<?>) type);
		} else {
			return canReflect(((ParameterizedType)type).getRawType());
		} 
	}

	public void deserialize(Type gt, Deserializer scanner, Map<K,V> autoValue, Property<Map<K,V>> prop)
			throws IOException, SyntaxError {
		ParameterizedType pt = (ParameterizedType) gt;
		
		try {
			Type keytype = pt.getActualTypeArguments()[0];
			Type valtype = pt.getActualTypeArguments()[1];

			boolean keystrong = Mirror.isStrongType(keytype);
			boolean valstrong = Mirror.isStrongType(valtype);
			
			Map<K,V> out = autoValue;
			if (out == null) {
				Mirror.instantiateInto(prop, false);
				out = prop.get();
			}
			
			if (Mirror.braceMaps) scanner.expect('{');
				
			while (scanner.nextToken() == (Mirror.braceMapElements?'{':'[')) {
				if (Mirror.braceMapElements)
					scanner.expect('[');
				scanner.expectWord("key");
				scanner.expect('=');

				Annotation[] keyans = Mirror.getInnerAnnotations(prop, 0);
				Annotation[] valans = Mirror.getInnerAnnotations(prop, 1);
				
				MapElement<K,V> elm = new MapElement<K,V>(out, keytype, keyans, valtype, valans, prop.context());
				
				if (!keystrong) scanner.expect('{');
				Mirror.unserializeValue(keytype, scanner, null, elm.key());
				if (!keystrong) scanner.expect('}');

				scanner.expect(']');

				if (!valstrong) scanner.expect('{');
				// performs put
				Mirror.unserializeValue(valtype, scanner, null, elm.val());
				if (!valstrong) scanner.expect('}');
				
				if (Mirror.braceMapElements)
					scanner.expect('}');
			}
			scanner.pushBack();

			if (Mirror.braceMaps) scanner.expect('}');
			
			prop.set(out, false);
		} catch (SyntaxError e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}

	public void serialize(Map<K,V> v, Serializer o) throws IOException {
		Map<K,V> elms = (Map<K,V>) v;
		
		if (Mirror.braceMaps)
			o.write("{\n".getBytes());
		o.indentin();
		for (Entry<? extends Object, ? extends Object> e : elms.entrySet()) {
			o.indent();
			if (Mirror.braceMapElements)
				o.write('{');
			o.write("[key=".getBytes());
			
			Object key = e.getKey();
			Object val = e.getValue();
			
			boolean keystrong = Mirror.isStrongType(key.getClass());
			boolean valstrong = Mirror.isStrongType(val.getClass());
			
			if (!keystrong) o.write('{');
			Mirror.serializeValue(e.getKey(), o);
			if (!keystrong) o.write('}');
			o.write("] ".getBytes());
			if (!valstrong) o.write('{');
			Mirror.serializeValue(e.getValue(), o);
			if (!valstrong) o.write('}');
			o.write("\n".getBytes());
			if (Mirror.braceMapElements)
				o.write('}');
		}
		o.indentout();

		if (Mirror.braceMaps) {
			o.indent();
			o.write("} ".getBytes());
		}
	}
	
	static class MapElement<K,V> {
		class KeyProp extends AbstractProperty<K, K, V> {
			KeyProp(Type type, Annotation[] ans) {
				super(type, "", ans);
			}
			
			public K get() {return key;}
			
			public void set(K val, boolean log) {
				boolean dirty = false;
				if (!dummyEntry) {
					if (log) {
						if (val != null)
							UndoLog.staticUndoLog().logChange(this, val, map.get(val), map.get(key));
						UndoLog.staticUndoLog().logChange(this, key, map.get(key), null);
					}
					if (map.containsKey(val)) 
						dirty = true;
					map.put(val, map.get(key));
					map.remove(key);
				}
				key = val;
				if (dirty && editor != null && editor.get() != null)
					editor.get().externallyUpdated();
			}
			
			public void performUndo(K arg, V val) {
				if (val == null)
					map.remove(arg);
				else
					map.put(arg, val);
				key = arg;
			}

			public Object context() {
				return context;
			}
		}
		
		class ValProp extends UndoableAbstractProperty<V> {
			ValProp(Type type, Annotation[] ans) {
				super(type, "", ans);
			}
			
			public V get() {return map.get(key);}
			
			public void set(V val) {
				dummyEntry = false;
				if (val != null)
					map.put(key, val);
				else
					map.remove(key);
			}

			public Object context() {
				return context;
			}
		}
		
		final Map<K,V> map;
		K key;
		final KeyProp kp;
		final ValProp vp;
		WeakReference<MyEditor<K,V>> editor;
		Object context;
		boolean dummyEntry;

		public MapElement(
				MyEditor<K,V> editor, Map<K,V> map, K key,
				Type keytype, Annotation[] keyans,
				Type valtype, Annotation[] valans,
				Object context)
		{
			this.editor = new WeakReference<MyEditor<K,V>>(editor);
			this.map = map;
			this.key = key;
			kp = new KeyProp(keytype, keyans);
			vp = new ValProp(valtype, valans);
			this.context = context;
			dummyEntry = false;
		}
		
		public MapElement(
				Map<K,V> map,
				Type keytype, Annotation[] keyans,
				Type valtype, Annotation[] valans,
				Object context)
		{
			this.editor = null;
			this.map = map;
			this.key = null;
			dummyEntry = true;
			kp = new KeyProp(keytype, keyans);
			vp = new ValProp(valtype, valans);
			this.context = context;
		}
		
		KeyProp key() {return kp;}
		ValProp val() {return vp;}
		
	}

	@SuppressWarnings("serial")
	static class MyEditor<K,V> extends Editor<Map<K,V>> implements ActionListener  {
		
		
		
		class Member extends Box implements ActionListener {
			Editor<?> keyed, valed;
			JCheckBox checkbox;
			boolean wasSelected;
			MapElement<K,V> elm;
			
			public Member(MapElement<K,V> elm) {
				super(BoxLayout.X_AXIS);
				this.elm = elm;
				checkbox = new JCheckBox();
				checkbox.addActionListener(this);
				checkbox.setAlignmentY(0);
				add(checkbox);
				keyed = Mirror.buildEditor(elm.key());
				keyed.setAlignmentY(0);
				add(keyed);
				valed = Mirror.buildEditor(elm.val());
				valed.setAlignmentY(0);
				add(valed);
			}
			
			public void actionPerformed(ActionEvent e) {
				if (checkbox.isSelected() == wasSelected) return;
				wasSelected = checkbox.isSelected();
				if (checkbox.isSelected())
					numSelected++;
				else
					numSelected--;
				deleteButton.setEnabled(numSelected>0);
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
		Type keyType;
		Type valType;
		Annotation[] keyans;
		Annotation[] valans;
		boolean nullOK;
		int numSelected;
		boolean collapsed = true;
		
		ArrayList<Member> contents = new ArrayList<Member>();
		
		Box contentBox;
		
		JButton deleteButton;
		
		protected MyEditor(Property<Map<K,V>> prop) {
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
			
			keyType = ((ParameterizedType)prop.getType()).getActualTypeArguments()[0];
			valType = ((ParameterizedType)prop.getType()).getActualTypeArguments()[1];

			keyans = Mirror.getInnerAnnotations(prop, 0);
			valans = Mirror.getInnerAnnotations(prop, 1);
			
			button = new JButton(IconCache.get("plus"));
			button.setActionCommand("add");
			button.addActionListener(this);
			buttonBox.add(button);

			deleteButton = new JButton(IconCache.get("x"));
			deleteButton.setActionCommand("remove");
			deleteButton.addActionListener(this);
			buttonBox.add(deleteButton);
			add(buttonBox);
			
			reload();
		}
		
		@SuppressWarnings("unchecked")
		void reload() {
			numSelected = 0;
			final Object v = prop.get();
			if (v == null) {
				contents = null;
			} else {
				contents = new ArrayList<Member>();
				Map map = (Map)prop.get();
				for(Object k : map.keySet())
					contents.add(new Member(new MapElement(this, map, k, keyType, keyans, valType, valans, prop.context())));
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
				else
					deleteButton.setEnabled(numSelected>0);
			} else {
				contentBox.add(new JLabel("null"));
				deleteButton.setEnabled(false);
			}
			contentBox.revalidate();
		}
		
		void commit(String event) {
			TempProperty<Map<K,V>> newprop = new TempProperty<Map<K,V>>(prop);
			if (contents == null) {
				newprop.set(null, false);
			} else {
				try {
					newprop.set(null, false);
					Mirror.instantiateInto(newprop, false);
					for(Member mem : contents)
						newprop.get().put(mem.elm.key().get(), mem.elm.val().get());
				} catch (Exception e) {
					throw new REThrow(e);
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
					Map<K,V> m = new HashMap<K,V>();
					MapElement<K,V> me = new MapElement<K,V>(m, keyType, keyans, valType, valans, prop.context());
					Mirror.instantiateInto(me.key(), true);
					Mirror.instantiateInto(me.val(), true); // does put
					contents.add(new Member(me));
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
		}
		
		@Override
		public void externallyUpdated() {
			if (prop.get() != oldval && (prop.get() == null || !prop.get().equals(oldval))) {
				reload();
			}
			super.externallyUpdated();
		}
	}
	
	public Editor<Map<K,V>> buildEditor(Property<Map<K,V>> p) {
		return new MyEditor<K,V>(p);
	}

	public boolean isStrongClass(Type t) {
		return false;
	}

	public void instantiateInto(Property<Map<K,V>> prop, Type t, boolean log) {
		try {
			Mirror.dumbInstantiateInto(prop, t, log, null);
		} catch (Exception e) {
			// might be because the field was just a Map<x>
			if (Utils.typeClass(t).isAssignableFrom(HashMap.class)) {
				Mirror.dumbInstantiateInto(prop, HashMap.class, log, null);
				return;
			}
			throw new REThrow(e);
		} 
	}
	

	static class QuickVal<K,V> extends UndoableAbstractProperty<V> {
		Map<K,V> map;
		K key;
		
		QuickVal(Map<K,V> map, K key) {
			super(Object.class, "", null);
			this.map = map;
			this.key = key;
		}
		
		public V get() {return map.get(key);}
		
		public void set(V val) {
			if (val != null)
				map.put(key, val);
			else
				map.remove(key);
		}

		public Object context() {
			return null;
		}
	}
	
	static public <K,V> void quickSet(Map<K, V> map, K key, V value, boolean log) {
		new QuickVal<K,V>(map, key).set(value, log);
	}
}
