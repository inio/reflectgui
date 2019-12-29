package util;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;

@SuppressWarnings("serial")
public abstract class SimpleAction extends AbstractAction {
	
	ArrayList<PropertyChangeListener> listeners;
	
	public SimpleAction(String name) {
		putValue(NAME, name);
	}

	public SimpleAction(String name, Icon icon) {
		this(name);
		putValue(SMALL_ICON, icon);
		putValue(LARGE_ICON_KEY, icon);
	}

	public SimpleAction(String name, Object shortcut, Icon icon) {
		this(name, icon);
		if (shortcut instanceof Character)
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke((Character) shortcut, InputEvent.CTRL_MASK));
		else if (shortcut instanceof KeyStroke)
			putValue(ACCELERATOR_KEY, (KeyStroke) shortcut);
	}

	public SimpleAction(String name, Object shortcut, Icon icon, String tip) {
		this(name, shortcut, icon);
		if (tip != null) {
			putValue(Action.SHORT_DESCRIPTION, tip);
		}
	}
	
	public void setIcon(Icon icon) {
		putValue(SMALL_ICON, icon);
		putValue(LARGE_ICON_KEY, icon);
	}
	
	private Action noName = new Action() {
		public void addPropertyChangeListener(PropertyChangeListener listener) {
			SimpleAction.this.addPropertyChangeListener(listener);
		}

		public Object getValue(String key) {
			if (key == NAME) return null;
			return SimpleAction.this.getValue(key);
		}

		public boolean isEnabled() {
			return SimpleAction.this.isEnabled();
		}

		public void putValue(String key, Object value) {
			SimpleAction.this.putValue(key, value);
		}

		public void removePropertyChangeListener(PropertyChangeListener listener) {
			SimpleAction.this.removePropertyChangeListener(listener);
		}

		public void setEnabled(boolean b) {
			SimpleAction.this.setEnabled(b);
		}

		public void actionPerformed(ActionEvent e) {
			SimpleAction.this.actionPerformed(e);
		}
	};
	
	public Action getNoName() {
		return noName;
	}
}
