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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import util.Utils;

class EnumMirror extends PrimitiveMirror<Enum<?>> {

	public boolean canReflect(Type type) {
		if (type instanceof Class) {
			return ((Class<?>) type).isEnum();
		} else {
			return canReflect(((ParameterizedType)type).getRawType());
		}
	}

	@SuppressWarnings("unchecked")
	public Object deserialize(Type gt, Deserializer scanner, Enum<?> autoValue)
			throws IOException, SyntaxError {
		Class<?> t = (Class<?>)gt;
		switch(scanner.nextToken()) {
		case StreamTokenizer.TT_WORD:
		case '"':
			try {
				return Enum.valueOf((Class<Enum>)t, scanner.sval);
			} catch (IllegalArgumentException  e) {
				throw new SyntaxError("\""+scanner.sval+"\" isn't a valid value for "+t.getCanonicalName());
			}
		default:
			throw new SyntaxError("expecting enum value");
		}
	}

	public void serialize(Enum<?> v, Serializer o) throws IOException {
		if (Mirror.namePattern.matcher(v.toString()).matches()) {
			o.write(v.toString().getBytes());
		} else {
			Mirror.serializeValue(v.toString(), o);
		}
	}
	
	@SuppressWarnings("serial")
	static class MyEditor extends Editor<Enum<?>> implements ActionListener {
		JComboBox menu;
		Class<? extends Enum<?>> etype;
		Enum<?> consts[];
		String strings[];
		@SuppressWarnings("unchecked")
		protected MyEditor(Property prop) {
			super(prop, BoxLayout.X_AXIS);
			add(new JLabel(label()));
			
			boolean nullOK = false;

			GUIInfo finfo = prop.getAnnotation(GUIInfo.class);
			if (finfo != null) for(GUIInfo.GUIInfoFlags f : finfo.flags()) {
				if (f == GUIInfo.GUIInfoFlags.nullOK)
					nullOK = true;
			}
			
			etype = (Class<? extends Enum<?>>) prop.getType();
			Enum<?> rawconsts[] = etype.getEnumConstants();
			if(nullOK) {
				consts = new Enum<?>[rawconsts.length+1];
				consts[0] = null;
				for(int i=0 ; i<rawconsts.length ; i++)
					consts[i+1] = rawconsts[i];
			} else {
				consts = rawconsts;
			}
			strings = new String[consts.length];
			int selectedIndex = 0;
			for(int i=0 ; i<consts.length ; i++) {
				if (consts[i] != null) {
					strings[i] = consts[i].toString();
					if (consts[i].equals(prop.get()))
						selectedIndex = i;
				} else {
					strings[i] = "<null>";
					if(prop.get() == null)
						selectedIndex = i;
				}
			}
			menu = new JComboBox(strings);
			menu.setSelectedIndex(selectedIndex);
			menu.addActionListener(this);
			add(menu);
			setMinimumSize(new Dimension(30, menu.getPreferredSize().height));
			setPreferredSize(new Dimension(0, menu.getPreferredSize().height));
			setMaximumSize(new Dimension(1000, menu.getPreferredSize().height));
		}
		
		public void actionPerformed(ActionEvent arg0) {
			Enum<?> newval = consts[menu.getSelectedIndex()];
			if (newval!=prop.get() && (newval == null || !newval.equals(prop.get()))) {
				UndoLog.staticUndoLog().begin("Change "+label());
				prop.set(newval, true);
				UndoLog.staticUndoLog().end();
				setDirty();
			}
		}
		
		@Override
		public void externallyUpdated() {
			if (prop.get() != null) {
				menu.setSelectedItem(prop.get().toString());
			} else {
				menu.setSelectedIndex(0);
			}
			super.externallyUpdated();
		}
	}
	
	public Editor<Enum<?>> buildEditor(Property<Enum<?>> prop) {
		return new MyEditor(prop);
	}

	public Object instantiate(Type t) {
		return Utils.typeClass(t).getEnumConstants()[0];
	}
}
