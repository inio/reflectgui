package reflect;

import java.awt.Container;
import java.util.ArrayList;

import javax.swing.Box;

@SuppressWarnings("serial")
public class Editor<T> extends Box {
	protected Editor<?> outer = null;
	protected ArrayList<Editor<?>> inners = new ArrayList<Editor<?>>();
	protected Property<T> prop;
	protected GUIInfo info;
	private boolean dirty;
	
	public void setDirty() {
		if (dirty) return;
		dirty = true;
		if (outer != null) outer.setDirty();
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	protected String label() {
		String out = prop.getName();
		if (info != null && info.name().length() > 0)
			out = info.name();
		return out;
	}
	
	protected String tooltip() {
		String out = null;
		if (info != null && info.tip().length() > 0)
			out = info.tip();
		return out;
	}

	public static interface UpdateListener {
		void changed(Editor<?> what);
	}
	
	protected ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();
	
	protected Editor(Property<T> prop, int axis) {
		super(axis);
		this.prop = prop;
		info = this.prop.getAnnotation(GUIInfo.class);
		if (info != null && info.tip().length()>0)
			setToolTipText(info.tip());
	}
	
	public void addListener(UpdateListener l) {
		listeners.add(l);
	}
	
	public void externallyUpdated() {
		for(Editor<?> e : inners)
			e.externallyUpdated();
	}
	
	public void updated(boolean f) {
		for(UpdateListener l : listeners)
			l.changed(this);
		if (null != outer)
			outer.updated(f);
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		
		Container at = getParent();
		while (at != null) {
			if(at instanceof Editor) {
				outer = (Editor<?>) at;
				outer.addInner(this);
				break;
			}
			at = at.getParent();
		}
	}
	
	@Override
	public void removeNotify() {
		super.removeNotify();
		
		if (outer != null)
			outer.removeInner(this);
	}

	protected void removeInner(Editor<?> editor) {
		inners.remove(editor);
	}

	protected void addInner(Editor<?> editor) {
		inners.add(editor);
	}
}
