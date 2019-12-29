package reflect;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.lang.reflect.Type;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import util.Utils;
import util.REThrow;

public class NumberMirror extends PrimitiveMirror<Object> {
	public boolean canReflect(Type type) {
		return Integer.class == type || int.class == type ||
				Long.class == type || long.class == type ||
				Float.class == type || float.class == type ||
				Double.class == type || double.class == type ||
				Boolean.class == type || boolean.class == type;
	}

	public Object deserialize(Type gt, Deserializer scanner, Object autoValue)
			throws IOException, SyntaxError {
		Class<?>type = (Class<?>) gt;
		if (Integer.class == type || int.class == type)
			if (scanner.nextToken() == Deserializer.TT_NUMBER)
				return new Integer((int)scanner.nval);
			else
				throw new SyntaxError("expecting number");
		if (Long.class == type || long.class == type)
			if (scanner.nextToken() == Deserializer.TT_NUMBER)
				return new Long((int)scanner.nval);
			else
				throw new SyntaxError("expecting number");
		if (Float.class == type || float.class == type)
			if (scanner.nextToken() == Deserializer.TT_NUMBER)
				return new Float((float)scanner.nval);
			else
				throw new SyntaxError("expecting number");
		if (Double.class == type || double.class == type)
			if (scanner.nextToken() == Deserializer.TT_NUMBER)
				return new Double((double)scanner.nval);
			else
				throw new SyntaxError("expecting number");
		if (Boolean.class == type || boolean.class == type)
			if (scanner.nextToken() == Deserializer.TT_WORD)
				return new Boolean(scanner.sval);
			else
				throw new SyntaxError("expecting boolean");
		throw new REThrow("blargh!!!");
	}

	public void serialize(Object v, Serializer o) throws IOException {
		o.write(v.toString().getBytes());
	}
	
	@SuppressWarnings("serial")
	static class NumEditor extends Editor<Object> implements ActionListener, FocusListener {
		JTextField field;
		Type type;
		protected NumEditor(Property<Object> prop) {
			super(prop, BoxLayout.X_AXIS);
			type = prop.getType();
			if (type == Integer.TYPE) type = Integer.class;
			else if (type == Double.TYPE) type = Double.class;
			else if (type == Float.TYPE) type = Float.class;
			else if (Integer.class.isAssignableFrom((Class<?>) type) ||
					Double.class.isAssignableFrom((Class<?>) type) ||
					Float.class.isAssignableFrom((Class<?>) type)) {}
			else throw new REThrow("Unexpected type");
			add(new JLabel(label()));
			field = new JTextField(prop.get().toString(), 15);
			field.setMaximumSize(new Dimension(1000, field.getPreferredSize().height));
			setMinimumSize(new Dimension(30, field.getPreferredSize().height));
			setPreferredSize(new Dimension(0, field.getPreferredSize().height));
			setMaximumSize(new Dimension(1000, field.getPreferredSize().height));
			field.addActionListener(this);
			field.addFocusListener(this);
			add(field);
			add(Box.createGlue());
		}
		
		void commit() {
			String strval = field.getText();
			Object newval;
			try {
				if (type == Integer.class)
					newval = new Integer(strval);
				else if (type == Float.class)
					newval = new Float(strval);
				else
					newval = new Double(strval);
				if (null==prop.get() || !prop.get().equals(newval)) {
					UndoLog.staticUndoLog().begin("Change "+label());
					prop.set(newval, true);
					UndoLog.staticUndoLog().end();
					setDirty();
				}
			} catch (NumberFormatException e) {
			}
			field.setText(prop.get().toString());
		}
		
		public void actionPerformed(ActionEvent arg0) {
			commit();
		}

		public void focusGained(FocusEvent arg0) {}

		public void focusLost(FocusEvent arg0) {
			commit();
		}

		@Override
		public void externallyUpdated() {
			String newval = prop.get().toString();
			if (!field.getText().equals(newval))
				field.setText(newval);
		}
	}
	
	@SuppressWarnings("serial")
	static class BoolEditor extends Editor<Object> implements ActionListener {
		JCheckBox field;
		protected BoolEditor(Property<Object> prop) {
			super(prop, BoxLayout.X_AXIS);
			field = new JCheckBox(label(), ((Boolean)prop.get()).booleanValue());
			field.addActionListener(this);
			add(field);
			add(Box.createHorizontalGlue());
			setMinimumSize(new Dimension(30, field.getPreferredSize().height));
			setPreferredSize(new Dimension(0, field.getPreferredSize().height));
			setMaximumSize(new Dimension(1000, field.getPreferredSize().height));
		}
		
		public void actionPerformed(ActionEvent arg0) {
			if (null==prop.get() || !prop.get().equals(field.isSelected())) {
				UndoLog.staticUndoLog().begin("Toggle "+label());
				prop.set(new Boolean(field.isSelected()), true);
				UndoLog.staticUndoLog().end();
				setDirty();
			}
		}
		
		@Override
		public void externallyUpdated() {
			if (!new Boolean(field.isSelected()).equals(prop.get()))
				field.setSelected(((Boolean)prop.get()).booleanValue());
			setDirty();
		}
	}
	
	public Editor<Object> buildEditor(Property<Object> prop) {
		if (Boolean.class.isAssignableFrom(prop.get().getClass()))
			return new BoolEditor(prop);
		else
			return new NumEditor(prop);
	}
	
	public Object instantiate(Type t) {
		try {
			return Utils.typeClass(t).newInstance();
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}
}
