package reflect;

import java.io.IOException;
import java.lang.reflect.Type;

public interface MagicMirror<T> {
	public boolean canReflect(Type type);
	public void serialize(T v, Serializer o) throws IOException;
	public void deserialize(Type t, Deserializer scanner, T autoValue, Property<T> prop) throws IOException, SyntaxError;

	public Editor<? extends T> buildEditor(Property<T> prop);
	public boolean isStrongClass(Type t);
	public void instantiateInto(Property<T> prop, Type t, boolean log);
}
