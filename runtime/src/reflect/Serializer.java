package reflect;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;

import util.REThrow;

public class Serializer extends FilterOutputStream {
	
	static class ObjHash {
		final Object target;
		static final Method hashCodeMethod;
		
		static {
			 try {
				hashCodeMethod = Object.class.getMethod("hashCode");
			} catch (Exception e) {
				throw new REThrow(e.getMessage());
			}
		}
		ObjHash(Object t) {target = t;}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof ObjHash)
				return ((ObjHash)obj).target == target;
			return false;
		}
		@Override
		public int hashCode() {
			try {
				return (Integer)hashCodeMethod.invoke(target);
			} catch (Exception e) {
				throw new REThrow(e.getMessage());
			}
		}
	}
	
	HashMap<ObjHash, Integer> objmap = new HashMap<ObjHash, Integer>();
	
	private int nextObjID;
	
	int lastb;
	
	@Override
	public void write(byte[] b) throws IOException {
		super.write(b);
		if (b.length > 0)
			lastb = b[b.length-1];
	} 
	
	@Override
	public void write(int b) throws IOException {
		super.write(b);
		lastb = b;
	}

	public Serializer(OutputStream out) {
		super(out);
	}
	
	int getNextObjID() {
		return nextObjID++;
	}

	private int indentLevel = 0;
	
	void indentin() {
		indentLevel++;
	}
	
	void indentout() {
		indentLevel--;
	}
	
	void indent() throws IOException {
		if (lastb == '\n') {
			for(int i=0 ; i<indentLevel ; i++)
				write("  ".getBytes());
		}
	}
	
	void serialize(Reflectable<?> obj) throws IOException {
		indentLevel = 0;
		obj.serialize(this);
	}

	public Integer getObjID(Object v) {
		return objmap.get(new ObjHash(v));
	}
	
	public int generateObjID(Object v) {
		objmap.put(new ObjHash(v), nextObjID);
		return nextObjID++;
	}
}
