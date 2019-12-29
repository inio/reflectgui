package reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class AutoProperty<T> extends UndoableAbstractProperty<T> {
	public T value;
	Object context;
	
	public AutoProperty(final T val, Type type, String name, Annotation[] ans, Object context) {
		super(type, name, ans);
		value = val;
		this.context = context;
	}
	
	public AutoProperty(final T val, Type type, String name, Annotation[] ans) {
		this(val, type, name, ans, null);
	}

	public AutoProperty(final T val, Type type, String name) {
		this(val, type, name, null);
	}
;
	public T get() {
		return value;
	}

	public void set(T val) {
		value = val;
	}

	public Object context() {
		return context;
	}
}
