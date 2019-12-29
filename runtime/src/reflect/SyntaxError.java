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

@SuppressWarnings("serial")
public class SyntaxError extends Exception {
	String prefix;
	public SyntaxError(String prefix, Throwable cause) {
		super(cause);
		this.prefix = prefix;
	}
	
	public SyntaxError(String message) {
		super(message);
		prefix = "";
	}
	
	@Override
	public String getLocalizedMessage() {
		if (getCause() != null)
			return prefix+getCause().getLocalizedMessage();
		else
			return prefix+super.getLocalizedMessage();
	}
}
