package reflect;

import java.io.IOException;
import java.lang.reflect.Type;

public abstract class PrimitiveMirror<T> implements MagicMirror<T> {
	abstract public Object deserialize(Type t, Deserializer scanner, T autoValue) throws IOException, SyntaxError;

	@SuppressWarnings("unchecked")
	public void deserialize(Type t, Deserializer scanner, T autoValue,
			Property<T> target) throws IOException, SyntaxError {
		target.set((T) deserialize(t, scanner, autoValue), false);
	}
	
	public boolean isStrongClass(Type t) {
		return true;
	}

	abstract public Object instantiate(Type t);
	
	@SuppressWarnings("unchecked")
	public void instantiateInto(Property<T> prop, Type t, boolean log) {
		prop.set((T) instantiate(t), log);
	}
}
