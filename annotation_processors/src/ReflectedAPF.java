package reflect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;

public class ReflectedAPF implements AnnotationProcessorFactory {
	@Override
	public AnnotationProcessor getProcessorFor(
			Set<AnnotationTypeDeclaration> types,
			AnnotationProcessorEnvironment env) {
		return new ReflectedAP( env );
	}

	@Override
	public Collection<String> supportedAnnotationTypes() {
		return Arrays.asList("reflect.Reflected");
	}

	@Override
	public Collection<String> supportedOptions() {
		return Collections.emptyList();
	}
}