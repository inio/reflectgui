package reflect;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.lang.reflect.Type;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;

import util.Utils;
import util.REThrow;

public class StringMirror extends PrimitiveMirror<String> {

	public boolean canReflect(Type type) {
		if (String.class == type) return true;
		return false;
	}

	public String deserialize(Type t, Deserializer scanner, String autoValue)
			throws IOException, SyntaxError {
		if (scanner.nextToken() != '"') throw new SyntaxError("Expecting string");
		return scanner.sval;
	}

	public void serialize(String v, Serializer o) throws IOException {
		o.write("\"".getBytes());
		o.write(((String)v).replace("\\", "\\\\").replace("\n", "\\n").replace("\"", "\\\"").getBytes());
		o.write("\"".getBytes());
	}
	
	@SuppressWarnings("serial")
	static class StrEditor extends Editor<String> implements ActionListener, FocusListener {
		JTextField field;
		protected StrEditor(Property<String> prop) {
			super(prop, BoxLayout.X_AXIS);
			add(new JLabel(label()));
			String value;
			if (prop.get() == null)
				value="";
			else
				value = prop.get().toString();
			field = new JTextField(value, 15);
			field.setMaximumSize(new Dimension(1000, field.getPreferredSize().height));
			field.addActionListener(this);
			field.addFocusListener(this);
			add(field);
		}
		
		void commit() {
			if (null==prop.get() || !prop.get().equals(field.getText())) {
				UndoLog.staticUndoLog().begin("Change "+label());
				prop.set(field.getText(), true);
				UndoLog.staticUndoLog().end();
				setDirty();
			}
		}
		
		@Override
		public void externallyUpdated() {
			String newval = (String) prop.get();
			if (!field.getText().equals(newval))
				field.setText(newval);
			super.externallyUpdated();
		}
		
		public void actionPerformed(ActionEvent arg0) {
			commit();
		}

		public void focusGained(FocusEvent arg0) {}

		public void focusLost(FocusEvent arg0) {
			commit();
		}
	}
	
	public Editor<String> buildEditor(Property<String> prop) {
		return new StrEditor(prop);
	}

	public boolean isStrongClass(Type t) {
		return true;
	}
	
	public Object instantiate(Type t) {
		try {
			return Utils.typeClass(t).newInstance();
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}
}
