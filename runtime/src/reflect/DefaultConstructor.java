package reflect;

import java.lang.reflect.Type;

public interface DefaultConstructor<T> {
	void instantiateInto(Property<T> target, Type t, boolean log);
}
