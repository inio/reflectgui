/*
 * Copyright 2009 Ian Rickard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

abstract public class AbstractProperty<T, K, V> implements Property<T>, UndoAware<K, V> {

	protected Annotation[] annotations;
	protected String name;
	protected Type type;

	public AbstractProperty(Type type, String name, Annotation[] annotations) {
		this.annotations = annotations;
		this.name = name;
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	public <aT extends Annotation> aT getAnnotation(Class<aT> cls) {
		if (annotations == null) return null;
		for(Annotation a : annotations)
			if (cls.isAssignableFrom(a.getClass()))
				return (aT) a;
		return null;
	}
	
	public Annotation[] getAnnotations() {
		return annotations;
	}
	
	public Annotation[] getDeclaredAnnotations() {
		return annotations;
	}
	
	public boolean isAnnotationPresent(
			Class<? extends Annotation> annotationClass) {
		return getAnnotation(annotationClass) != null;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

}