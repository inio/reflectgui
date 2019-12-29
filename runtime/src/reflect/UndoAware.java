package reflect;

public interface UndoAware<A, T> {
	public void performUndo(A arg, T val);
}
