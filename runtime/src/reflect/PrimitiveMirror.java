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

import java.io.IOException;
import java.lang.reflect.Type;

public abstract class PrimitiveMirror<T> implements MagicMirror<T> {
	abstract public Object deserialize(Type t, Deserializer scanner, T autoValue) throws IOException, SyntaxError;

	@SuppressWarnings("unchecked")
	public void deserialize(Type t, Deserializer scanner, T autoValue,
			Property<T> target) throws IOException, SyntaxError {
		target.set((T) deserialize(t, scanner, autoValue), false);
	}
	
	public boolean isStrongClass(Type t) {
		return true;
	}

	abstract public Object instantiate(Type t);
	
	@SuppressWarnings("unchecked")
	public void instantiateInto(Property<T> prop, Type t, boolean log) {
		prop.set((T) instantiate(t), log);
	}
}
