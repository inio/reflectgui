package reflect;

import java.util.ArrayList;
import java.util.Map.Entry;

import com.sun.mirror.apt.*;
import com.sun.mirror.declaration.*;
import com.sun.mirror.util.SourcePosition;

public class TupleAP implements AnnotationProcessor {
	AnnotationProcessorEnvironment env;
	AnnotationTypeDeclaration mytype;
	TypeDeclaration tupletype;
	AnnotationTypeElementDeclaration valueelm;
	
	public TupleAP(AnnotationProcessorEnvironment env) {
		this.env = env;
		mytype = (AnnotationTypeDeclaration) env.getTypeDeclaration("reflect.TupleField");
		tupletype = env.getTypeDeclaration("reflect.Tuple");
		for(AnnotationTypeElementDeclaration elm : mytype.getMethods()) {
			if (elm.getSimpleName() == "value")
				valueelm = elm;
		}
	}

	@Override
	public void process() {
		for(Declaration d : env.getDeclarationsAnnotatedWith(mytype))
		{
			ProcessDecl(d);
		}
		
	}

	private void ProcessDecl(Declaration rawd) {
		AnnotationMirror an = null;
		for(AnnotationMirror a : rawd.getAnnotationMirrors()) {
			if (a.getAnnotationType().getDeclaration().equals(mytype))
				an = a;
		}
		if (an == null) {
			env.getMessager().printError(rawd.getPosition(), "failed to find @TupleField annotation");
			return;
		}
		if (!(rawd instanceof FieldDeclaration)) {
			env.getMessager().printError(an.getPosition(), "@TupleField only applies to class fields");
			return;
		}
		FieldDeclaration d = (FieldDeclaration) rawd;
		
		ArrayList<ClassDeclaration> parents = new ArrayList<ClassDeclaration>();
		
		boolean isTuple = false;
		TypeDeclaration at = d.getDeclaringType();
		while (at != null) {
			if (at.equals(tupletype)) {
				isTuple = true;
				break;
			}
			parents.add((ClassDeclaration) at);
			try {
				at = ((ClassDeclaration)at).getSuperclass().getDeclaration();
			} catch (NullPointerException e) {
				at = null;
			}
		}
		
		
		if (!isTuple) {
			env.getMessager().printError(an.getPosition(), "@TupleField only applies to members of descendents of reflect.Tuple");
			return;
		}
		
		int value = getValue(an);
		
		String dups = "";
		
		for(ClassDeclaration cls : parents) {
			for(FieldDeclaration fld : cls.getFields()) {
				for(AnnotationMirror fld_an : fld.getAnnotationMirrors()) {
					if (fld_an.getAnnotationType().getDeclaration().equals(mytype)) {
						if (fld_an.equals(an)) continue;
						if (getValue(fld_an) == value)
							dups = dups+" "+cls.getQualifiedName()+"."+fld.getSimpleName();
					}
				}
			}
		}
		if (dups.length() > 0)
			env.getMessager().printError(
				getValuePos(an),
				"duplicate tuple field order (conflicts with"+dups+")");
	}

	private int getValue(AnnotationMirror an) {
		for(Entry<AnnotationTypeElementDeclaration, AnnotationValue> e : an.getElementValues().entrySet()) {
			if (e.getKey().getSimpleName().equals("value"))
				return ((Integer)e.getValue().getValue()).intValue();
		}
		env.getMessager().printNotice("Could't find value value");
		return 0;
	}

	private SourcePosition getValuePos(AnnotationMirror an) {
		for(Entry<AnnotationTypeElementDeclaration, AnnotationValue> e : an.getElementValues().entrySet()) {
			if (e.getKey().getSimpleName().equals("value"))
				return e.getValue().getPosition();
		}
		env.getMessager().printNotice("Could't find value value");
		return null;
	}
}
