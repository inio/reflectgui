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

public abstract class UndoableAbstractProperty<T> extends AbstractProperty<T, Object, T> {
	public UndoableAbstractProperty(Type type, String name, Annotation[] annotations) {
		super(type, name, annotations);
		
		GUIInfo info = getAnnotation(GUIInfo.class);
		if (info != null && info.name().length() > 0)
			name = info.name();
	}
	
	final public void set(T val, boolean log) {
		if (log)
			UndoLog.staticUndoLog().logChange(this, get(), val);
		set(val);
	}

	abstract protected void set(T val);

	public void performUndo(Object arg, T val) {
		set(val, false);
	}
}
