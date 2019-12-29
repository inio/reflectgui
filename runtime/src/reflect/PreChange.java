package reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 *	Function to call when annotated field is changed.  Named function
 *  should have prototype Type name(Type).  On undo/redo, hook will be
 *  called but returned value is not honored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface PreChange {
	String value();
}
