package reflect;

import java.lang.reflect.Type;

public class NullDefault implements DefaultConstructor<Object> {

	public void instantiateInto(Property<Object> target, Type t, boolean log) {
		target.set(null, log);
	}
}
