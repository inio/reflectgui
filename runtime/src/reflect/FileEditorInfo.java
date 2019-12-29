package reflect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface FileEditorInfo {
	public enum Mode {openFile, openFolder, saveFile};
	public Mode mode();
	public String[] extensions() default {};
	public String prompt() default "";
}
