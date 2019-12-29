package reflect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.sun.mirror.declaration.ConstructorDeclaration;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.EnumDeclaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.InterfaceDeclaration;
import com.sun.mirror.declaration.Modifier;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.ArrayType;
import com.sun.mirror.type.ClassType;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.EnumType;
import com.sun.mirror.type.PrimitiveType;
import com.sun.mirror.type.TypeMirror;
import com.sun.mirror.util.SourcePosition;

public class ReflectedAP implements AnnotationProcessor {
	AnnotationProcessorEnvironment env;
	AnnotationTypeDeclaration mytype;
	AnnotationTypeElementDeclaration valueelm;
	TypeDeclaration tupletype;
	
	final static Pattern namePattern = Pattern.compile("(?:[a-zA-Z_][a-zA-Z0-9_]*::)*[a-zA-Z_][a-zA-Z0-9_]*");
	
	public ReflectedAP(AnnotationProcessorEnvironment env) {
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
		for (Declaration d : env.getDeclarationsAnnotatedWith(mytype)) {
			if (d instanceof ClassDeclaration)
				ProcessClass((ClassDeclaration)d);
			else if (d instanceof FieldDeclaration)
				ProcessField((FieldDeclaration)d);
			else
				env.getMessager().printError(d.getPosition(),
						"this kind of declaration cannot be @Reflected");
		}
	}

	private void ProcessClass(ClassDeclaration d) {
		AnnotationMirror an = null;
		for(AnnotationMirror a : d.getAnnotationMirrors()) {
			if (a.getAnnotationType().getDeclaration().equals(mytype))
				an = a;
		}
		if (an == null) {
			env.getMessager().printError(d.getPosition(), "failed to find @Reflected annotation");
			return;
		}
		
		
		for (ConstructorDeclaration ctor : d.getConstructors()) {
			if (ctor.getParameters().size() == 0)
				return;
			/*	if (!ctor.getModifiers().contains(Modifier.PUBLIC)) {
					env.getMessager().printError(an.getPosition(), "reflected types must have a visible parameterless constructor");
					return;
				}*/
					
		}
		
		boolean hasnames = false;
		for(String name : getValues(an)) {
			hasnames = true;
			if(!namePattern.matcher(name).matches()) {
				env.getMessager().printError(getValuePos(an, name), "invalid reflection name");
				return;
			}
		}
		if (!hasnames) {
			env.getMessager().printError(an.getPosition(), "reflected classes must be named");
			return;
		}
	}
	
	static final List<String> builtins = Arrays.asList(
		Integer.class.getCanonicalName(),
		Float.class.getCanonicalName(),
		Double.class.getCanonicalName(),
		Boolean.class.getCanonicalName(),
		String.class.getCanonicalName()
	);
	
	
	private void ProcessField(FieldDeclaration d) {
		AnnotationMirror an = null;
		for(AnnotationMirror a : d.getAnnotationMirrors()) {
			try {
				if (a.getAnnotationType().getDeclaration().equals(mytype))
					an = a;
			} catch (NullPointerException e) {}
		}
		if (an == null) {
			env.getMessager().printError(d.getPosition(), "failed to find @Reflected annotation");
			return;
		}
	/*	if (!d.getModifiers().contains(Modifier.PUBLIC)) {
			env.getMessager().printError(an.getPosition(), "only public fields can be reflected");
			return;
		}*/
		TypeMirror rawtype = d.getType();
		if (rawtype instanceof ArrayType) {
			rawtype = ((ArrayType)rawtype).getComponentType();
		}
		if (rawtype instanceof PrimitiveType) {
			PrimitiveType prim = (PrimitiveType)d.getType();
			switch (prim.getKind()) {
			case BOOLEAN:
			case LONG:
			case INT:
			case FLOAT:
			case DOUBLE:
				return;
			default:
				env.getMessager().printError(an.getPosition(), "This primitive type cannot be reflected.");
			}
		}
		TypeDeclaration typedecl = ((DeclaredType)rawtype).getDeclaration();
		if (typedecl == null) return;
	/*	if (!typedecl.getModifiers().contains(Modifier.PUBLIC)) {
			env.getMessager().printError(an.getPosition(), "Only public types can be reflected");
			return;
		}*/
		if (typedecl instanceof EnumDeclaration) {
			return;
		}
		if (typedecl instanceof InterfaceDeclaration) {
			env.getMessager().printError(an.getPosition(), "Reflected fields must be of concrete types");
			return;
		}
		try {if (((ClassType)rawtype).getContainingType() != null) {
			if (!typedecl.getModifiers().contains(Modifier.STATIC)) {
				env.getMessager().printError(an.getPosition(), "non-static inner classes cannot be reflected");
				return;
			}
		}} catch (Exception e) {
		}
		
		for (ConstructorDeclaration ctor : ((ClassDeclaration)typedecl).getConstructors()) {
			if (ctor.getParameters().size() == 0)
				if (!ctor.getModifiers().contains(Modifier.PUBLIC)) {
					env.getMessager().printError(an.getPosition(), "reflected types must have a visible parameterless constructor");
					return;
				}
					
		}
		
		List<String> names;
		names = getValues(an);
		if (names.size() == 0)
			names = Arrays.asList(d.getSimpleName());
		
		if (!canReflect(d.getType())) {
			env.getMessager().printError(an.getPosition(), "cannot reflect this type of primitive");
			return;
		}
	}
	
	boolean canReflect(TypeMirror t) {
		if (t instanceof PrimitiveType) {
			switch(((PrimitiveType)t).getKind()) {
			case INT:
			case FLOAT:
			case DOUBLE:
			case BOOLEAN:
				return true;
			default:
				return false;
			}
		} else if (t instanceof EnumType) {
			return true;
		} else if (t instanceof ArrayType) {
			return canReflect(((ArrayType)t).getComponentType());
		} else if (t instanceof ClassType) {
			return true;
		} else {
			return false;
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
	
	@SuppressWarnings("unchecked")
	private SourcePosition getValuePos(AnnotationMirror an, String val) {
		for(Entry<AnnotationTypeElementDeclaration, AnnotationValue> e : an.getElementValues().entrySet())
			if (e.getKey().getSimpleName().equals("value"))
				for(AnnotationValue v : (Collection<AnnotationValue>)e.getValue().getValue())
					if (v.getValue().equals(val)) return v.getPosition();
		return null;
	}
}
