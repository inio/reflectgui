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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import util.REThrow;

public class FieldProperty<T> implements Property<T> {
	private Object obj;
	protected Field field;
	
	public Object getObj() {
		return obj;
	}

	/* (non-Javadoc)
	 * @see reflect.PropertyI#getAnnotation(java.lang.Class)
	 */
	public <Te extends Annotation> Te getAnnotation(Class<Te> cls) {
		return field.getAnnotation(cls);
	}
	
	public Annotation[] getAnnotations() {
		return field.getAnnotations();
	}
	
	public Annotation[] getDeclaredAnnotations() {
		return field.getDeclaredAnnotations();
	}
	
	public boolean isAnnotationPresent(
			Class<? extends Annotation> annotationClass) {
		return field.isAnnotationPresent(annotationClass);
	}
	
	public FieldProperty(Object obj, Field field) {
		this.obj = obj;
		this.field = field;
	}
	
	public FieldProperty(Object obj, String name) {
		this.obj = obj;
		try {
			this.field = Mirror.getField(obj.getClass(), name);
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see reflect.PropertyI#getType()
	 */
	public Type getType() {
		return field.getGenericType();
	}
	
	/* (non-Javadoc)
	 * @see reflect.PropertyI#getName()
	 */
	public String getName() {
		return Mirror.getDisplayName(field);
	}
	
	/* (non-Javadoc)
	 * @see reflect.PropertyI#get()
	 */
	@SuppressWarnings("unchecked")
	public T get() {
		try {
			return (T) field.get(obj);
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see reflect.PropertyI#set(java.lang.Object, boolean)
	 */
	@SuppressWarnings("unchecked")
	public void set(T val, boolean log) {
		PreChange  hook = field.getAnnotation(PreChange.class);
		if (hook != null) {
			try {
				Method m = Mirror.getMethod(obj.getClass(), hook.value(), field.getType());
				m.setAccessible(true);
				val = (T) m.invoke(obj, val);
			} catch (Exception e) {
				System.err.println("exception running change hook for "+
						obj.getClass().getCanonicalName()+"."+field.getName()+": "+e);
			}
		}
		Class<?> c = obj.getClass();
		while(c != Object.class) {
			hook = c.getAnnotation(PreChange.class);
			if (hook != null) {
				try {
					Mirror.getMethod(obj.getClass(), hook.value()).invoke(obj);
				} catch (Exception e) {
					System.err.println("exception running change hook for "+
							obj.getClass().getCanonicalName()+"."+field.getName()+": "+e);
				}
			}
			c = c.getSuperclass();
		}
		if (log)
			UndoLog.staticUndoLog().logChange(obj, field, get(), val);
		try {
			field.set(obj, val);
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}
	
	/* (non-Javadoc)
	 * @see reflect.PropertyI#set(java.lang.Object)
	 */
	final public void set(T val) {
		set(val, true);
	}

	public static void quickSet(Object obj, String field,
			Object newval, Boolean log) {
		try {
			FieldProperty<Object> p = new FieldProperty<Object>(obj, field);
			p.set(newval, log);
		} catch (Exception e) {
			throw new REThrow(e);
		}
	}

	public Object context() {
		return obj;
	}
}
