package reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public abstract class UndoableAbstractProperty<T> extends AbstractProperty<T, Object, T> {
	public UndoableAbstractProperty(Type type, String name, Annotation[] annotations) {
		super(type, name, annotations);
		
		GUIInfo info = getAnnotation(GUIInfo.class);
		if (info != null && info.name().length() > 0)
			name = info.name();
	}
	
	final public void set(T val, boolean log) {
		if (log)
			UndoLog.staticUndoLog().logChange(this, get(), val);
		set(val);
	}

	abstract protected void set(T val);

	public void performUndo(Object arg, T val) {
		set(val, false);
	}
}
