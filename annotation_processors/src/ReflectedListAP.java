package reflect;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.ClassDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.TypeDeclaration;

public class ReflectedListAP implements AnnotationProcessor {
	AnnotationProcessorEnvironment env;
	AnnotationTypeDeclaration mytype;
	AnnotationTypeElementDeclaration valueelm;
	TypeDeclaration tupletype;
	
	HashMap<String, String> classmap;
	
	final static Pattern namePattern = Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*::)*[a-zA-Z_][a-zA-Z0-9_]*");
	
	public ReflectedListAP(AnnotationProcessorEnvironment env) {
		this.env = env;
		mytype = (AnnotationTypeDeclaration) env.getTypeDeclaration("reflect.Reflected");
		tupletype = env.getTypeDeclaration("reflect.Tuple");
		for(AnnotationTypeElementDeclaration elm : mytype.getMethods()) {
			if (elm.getSimpleName() == "value")
				valueelm = elm;
		}
		
		
	}
	
	@Override
	public void process() {
		classmap = new HashMap<String, String>();
		for (Declaration d : env.getDeclarationsAnnotatedWith(mytype)) {
			if (d instanceof ClassDeclaration)
				ProcessClass((ClassDeclaration)d);
		}
		if (classmap.size() > 0) try {
			PrintWriter classfile = env.getFiler().createSourceFile("reflect.NamedTypes");
			classfile.println("package reflect;");
			classfile.println("import java.util.HashMap;");
			classfile.println("class NamedTypes {");
			classfile.println("\tstatic HashMap<String, Class<?>> types = new HashMap<String, Class<?>>();");
			classfile.println("\tstatic {");
			for(Entry<String, String> e : classmap.entrySet())
				classfile.println("\t\ttypes.put(\""+e.getKey()+"\", "+e.getValue()+".class);");
			classfile.println("\t}");
			classfile.println("}");
			classfile.close();
			classfile = null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}

	private void ProcessClass(ClassDeclaration d) {
		AnnotationMirror an = null;
		for(AnnotationMirror a : d.getAnnotationMirrors()) {
			if (a.getAnnotationType().getDeclaration().equals(mytype))
				an = a;
		}
		if (an == null) {
			return;
		}
		
		TypeDeclaration at = d;
		while(at != null) {
			if (!at.getModifiers().contains(Modifier.PUBLIC)) {
				return;
			}
			at = at.getDeclaringType();
		}
		
		for(String name : getValues(an)) {
			classmap.put(name, d.getQualifiedName());
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<String> getValues(AnnotationMirror an) {
		ArrayList<String> vals = new ArrayList<String>();
		for(Entry<AnnotationTypeElementDeclaration, AnnotationValue> e : an.getElementValues().entrySet())
			if (e.getKey().getSimpleName().equals("value"))
				for(AnnotationValue v : (Collection<AnnotationValue>)e.getValue().getValue())
					vals.add((String)v.getValue());
		return vals;
	}
}
