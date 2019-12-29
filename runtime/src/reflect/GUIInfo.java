package reflect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface GUIInfo  {
	public enum GUIInfoFlags {
		classLocked, nullOK
	}
	
	String name() default "";
	String tip() default "";
	boolean hide() default false;
	int order() default 1000;
	@SuppressWarnings("unchecked")
	Class<? extends Editor> editor() default Editor.class;
	EditorInfo editorInfo() default @EditorInfo;
	GUIInfoFlags[] flags() default {};
	GUIInfoFlags[] innerflags() default {};
	
	// not strictly GUI but ...
	boolean constructWithContext() default false;
	@SuppressWarnings("unchecked")
	Class<? extends DefaultConstructor> constructor() default DefaultConstructor.class;
}
