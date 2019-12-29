package reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InnerGUIInfo  {
	@Retention(RetentionPolicy.RUNTIME)
	@Target({})
	public static @interface I {
		int[] path();
		GUIInfo info();
	}
	I[] value();
}
