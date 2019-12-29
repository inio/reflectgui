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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


@Reflected("canvas::ReflectExample")
public class ReflectExample {
	@Reflected
	public int theint;
	@Reflected
	public boolean thebool;
	@Reflected
	public float thefloat;
	@Reflected
	public double thedouble;

	@Reflected
	public String string1, string2, string3;
	
	public enum testEnum {
		value1,
		value2,
		value3
	};

	@Reflected
	public testEnum e1, e2;
	

	@Reflected("canvas::ReflectExample::Base")
	public static class Base {
		@Reflected
		public int a = 1;
	}

	@Reflected("canvas::ReflectExample::Sub")
	public static class Sub extends Base {
		@Reflected
		public int b = 2;
	}
	
	@Reflected
	public Base arrayAndSubclassTest[];
	
	@Reflected
	public HashMap<String, ArrayList<String>> maptest;
	
	public ReflectExample() {
	}
	
	void init() {
		theint = 42;
		thebool = true;
		thefloat = (float) (1/Math.sqrt(2));
		thedouble = Math.PI;
		
		string1 = "one";
		string2 = "two \"words\"";
		string3 = "one line\nunder\\over";
		
		e1 = testEnum.value2;
		e2 = testEnum.value3;
		
		arrayAndSubclassTest = new Base[] {new Base(), new Sub(), new Base()};
		
		maptest = new HashMap<String, ArrayList<String>>();
		maptest.put("directions", new ArrayList<String>(Arrays.asList("up", "up", "down", "down", "left", "right", "left", "right")));
		maptest.put("buttons", new ArrayList<String>(Arrays.asList("B", "A", "Start")));
	}
}