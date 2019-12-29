package reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

public class TempProperty<T> implements Property<T> {
	Property<T> target;
	
	boolean set = false;
	T val;
	
	void commit(boolean log) {
		target.set(val, log);
	}
	
	TempProperty(Property<T> target) {
		this.target = target;
	}

	public Object context() {
		return target.context();
	}

	public T get() {
		if (set)
			return val;
		else
			return target.get();
	}

	public String getName() {
		return target.getName();
	}

	public Type getType() {
		return target.getType();
	}

	public void set(T val, boolean log) {
		this.val = val;
		set = true;
	}

	public <Te extends Annotation> Te getAnnotation(Class<Te> annotationClass) {
		return target.getAnnotation(annotationClass);
	}

	public Annotation[] getAnnotations() {
		return target.getAnnotations();
	}

	public Annotation[] getDeclaredAnnotations() {
		return target.getDeclaredAnnotations();
	}

	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return target.isAnnotationPresent(annotationClass);
	}
	
}
