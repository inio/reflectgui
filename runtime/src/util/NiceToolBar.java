package util;

import java.awt.Insets;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JToolBar;


@SuppressWarnings("serial")
public class NiceToolBar extends JToolBar {
	public NiceToolBar() {
		super();
	}

	public NiceToolBar(int orientation) {
		super(orientation);
	}

	public NiceToolBar(String name, int orientation) {
		super(name, orientation);
	}

	public NiceToolBar(String name) {
		super(name);
	}
	
	@Override
	public JButton add(Action a) {
		JButton b = super.add(a);
		b.setFocusPainted(false);
		b.setMargin(new Insets(0,0,0,0));
		return b;
	}
}
