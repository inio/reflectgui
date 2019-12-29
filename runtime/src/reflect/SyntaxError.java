package reflect;

@SuppressWarnings("serial")
public class SyntaxError extends Exception {
	String prefix;
	public SyntaxError(String prefix, Throwable cause) {
		super(cause);
		this.prefix = prefix;
	}
	
	public SyntaxError(String message) {
		super(message);
		prefix = "";
	}
	
	@Override
	public String getLocalizedMessage() {
		if (getCause() != null)
			return prefix+getCause().getLocalizedMessage();
		else
			return prefix+super.getLocalizedMessage();
	}
}
