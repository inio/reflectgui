package reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

abstract public class AbstractProperty<T, K, V> implements Property<T>, UndoAware<K, V> {

	protected Annotation[] annotations;
	protected String name;
	protected Type type;

	public AbstractProperty(Type type, String name, Annotation[] annotations) {
		this.annotations = annotations;
		this.name = name;
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	public <aT extends Annotation> aT getAnnotation(Class<aT> cls) {
		if (annotations == null) return null;
		for(Annotation a : annotations)
			if (cls.isAssignableFrom(a.getClass()))
				return (aT) a;
		return null;
	}
	
	public Annotation[] getAnnotations() {
		return annotations;
	}
	
	public Annotation[] getDeclaredAnnotations() {
		return annotations;
	}
	
	public boolean isAnnotationPresent(
			Class<? extends Annotation> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

}