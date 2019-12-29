package reflect;

import java.io.IOException;
import java.lang.reflect.Type;

import util.Utils;

public class ReflectableMirror<T extends Reflectable<?>> implements MagicMirror<T> {
	public boolean canReflect(Type type) {
		if(Reflectable.class.isAssignableFrom(Utils.typeClass(type)))
			return true;
		return false;
	}

	public void deserialize(Type t, Deserializer scanner, T autoValue, Property<T> target)
			throws IOException, SyntaxError {
		TempProperty<T> tmpval = new TempProperty<T>(target);
		Mirror.instantiateInto(tmpval, t, autoValue, false);
		tmpval.get().deserialize(scanner, tmpval.context());
		tmpval.commit(false);
	}

	public void serialize(T v, Serializer o) throws IOException {
		v.serialize(o);
	}
	
	@SuppressWarnings("unchecked")
	public Editor<T> buildEditor(Property<T> prop) {
		if (prop.get() == null) Mirror.instantiateInto(prop, true);
		return (Editor<T>) prop.get().buildEditor(prop);
	}

	public boolean isStrongClass(Type t) {
		return true;
	}

	public void instantiateInto(Property<T> target, Type t, boolean log) {
		Mirror.dumbInstantiateInto(target, t, log, null);
	}
}
