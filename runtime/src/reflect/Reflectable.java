package reflect;

import java.io.IOException;

public interface Reflectable<T extends Reflectable<?>> {
	public abstract void serialize(Serializer o) throws IOException;
	public abstract void deserialize(Deserializer scanner, Object context) throws IOException, SyntaxError;
	
	public abstract Editor<T> buildEditor(Property<T> prop);
}
