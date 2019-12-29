package util;

import java.util.HashMap;

import javax.swing.ImageIcon;

public class IconCache {
	static private HashMap<String, ImageIcon> cache = new HashMap<String, ImageIcon>();
	
	public static ImageIcon get(String name) {
		if (cache.containsKey(name))
			return cache.get(name);

		try {
			ImageIcon out = new ImageIcon(IconCache.class.getClassLoader().getResource("icons/"+name+".png"));
			cache.put(name, out);
			return out;
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}
}
