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

public class AutoProperty<T> extends UndoableAbstractProperty<T> {
	public T value;
	Object context;
	
	public AutoProperty(final T val, Type type, String name, Annotation[] ans, Object context) {
		super(type, name, ans);
		value = val;
		this.context = context;
	}
	
	public AutoProperty(final T val, Type type, String name, Annotation[] ans) {
		this(val, type, name, ans, null);
	}

	public AutoProperty(final T val, Type type, String name) {
		this(val, type, name, null);
	}
;
	public T get() {
		return value;
	}

	public void set(T val) {
		value = val;
	}

	public Object context() {
		return context;
	}
}
