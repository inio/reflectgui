package util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

@SuppressWarnings("serial")
public class NiceMenuBar extends JMenuBar {
	public HashMap<String, ArrayList<MenuItem>> items = new HashMap<String, ArrayList<MenuItem>>();
	abstract public class MenuItem {
		protected JMenuItem item;
		public boolean enabled() {return true;}
		public abstract void go();
		
		protected void update() {
			item.setEnabled(enabled());
		}
		
		public MenuItem(JMenu menu, String name, Object shortcut) {
			item = new JMenuItem(name);
			if (shortcut instanceof Character)
				item.setAccelerator(KeyStroke.getKeyStroke((Character) shortcut, InputEvent.CTRL_MASK));
			else if (shortcut instanceof KeyStroke)
				item.setAccelerator((KeyStroke) shortcut);
			item.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					MenuItem.this.go();
				}
			});
			menu.add(item);
			if (NiceMenuBar.this.items.containsKey(menu.getName())) 
				NiceMenuBar.this.items.get(menu.getName()).add(this);
			else
				NiceMenuBar.this.items.put(menu.getName(), new ArrayList<MenuItem>(Arrays.asList(this)));
		}
		
		public MenuItem(JMenu menu, String name) {
			this(menu, name, null);
		}
	}
	
	@Override
	public JMenu add(JMenu c) {
		c.addMenuListener(new MenuListener() {
			public void menuCanceled(MenuEvent e) {}
			public void menuDeselected(MenuEvent e) {}

			public void menuSelected(MenuEvent e) {
				for(MenuItem i : NiceMenuBar.this.items.get(((JMenu)e.getSource()).getName()))
					i.update();
			}
		});
		return super.add(c);
	}}
