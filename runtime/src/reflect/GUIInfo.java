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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface GUIInfo  {
	public enum GUIInfoFlags {
		classLocked, nullOK
	}
	
	String name() default "";
	String tip() default "";
	boolean hide() default false;
	int order() default 1000;
	@SuppressWarnings("unchecked")
	Class<? extends Editor> editor() default Editor.class;
	EditorInfo editorInfo() default @EditorInfo;
	GUIInfoFlags[] flags() default {};
	GUIInfoFlags[] innerflags() default {};
	
	// not strictly GUI but ...
	boolean constructWithContext() default false;
	@SuppressWarnings("unchecked")
	Class<? extends DefaultConstructor> constructor() default DefaultConstructor.class;
}
