package util;

import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("serial")
public class REThrow extends RuntimeException {
	static Throwable getCause(Throwable e) {
		if (e instanceof RuntimeException && e.getCause()!=null)
			return e.getCause();
		if (e instanceof InvocationTargetException)
			return ((InvocationTargetException)e).getTargetException();
		return e;
	}
	
	public REThrow(Throwable e) {
		super(getCause(e));
	}

	public REThrow(String message) {
		super(new Exception(message));
	}
}
