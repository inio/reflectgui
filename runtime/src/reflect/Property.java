package reflect;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

public interface Property<T> extends AnnotatedElement {

	public Type getType();

	public String getName();

	public T get();

	public void set(T val, boolean log);
	
	public Object context();
}