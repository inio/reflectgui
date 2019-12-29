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

public class TempProperty<T> implements Property<T> {
	Property<T> target;
	
	boolean set = false;
	T val;
	
	void commit(boolean log) {
		target.set(val, log);
	}
	
	TempProperty(Property<T> target) {
		this.target = target;
	}

	public Object context() {
		return target.context();
	}

	public T get() {
		if (set)
			return val;
		else
			return target.get();
	}

	public String getName() {
		return target.getName();
	}

	public Type getType() {
		return target.getType();
	}

	public void set(T val, boolean log) {
		this.val = val;
		set = true;
	}

	public <Te extends Annotation> Te getAnnotation(Class<Te> annotationClass) {
		return target.getAnnotation(annotationClass);
	}

	public Annotation[] getAnnotations() {
		return target.getAnnotations();
	}

	public Annotation[] getDeclaredAnnotations() {
		return target.getDeclaredAnnotations();
	}

	public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
		return target.isAnnotationPresent(annotationClass);
	}
	
}
