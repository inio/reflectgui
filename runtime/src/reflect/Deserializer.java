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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.HashMap;


public class Deserializer extends StreamTokenizer{
	HashMap<Integer, Object> loadedObjects = new HashMap<Integer, Object>();
	
	public Deserializer(Reader in) throws IOException {
		super(new BufferedReader(in));
		resetSyntax();
		commentChar('!');
		wordChars('A', 'Z');
		wordChars('a', 'z');
		wordChars('_', '_');
		wordChars(':', ':');
		parseNumbers();
		whitespaceChars(' ', ' ');
		whitespaceChars('\n', '\n');
		whitespaceChars('\r', '\r');
		whitespaceChars('\t', '\t');
		quoteChar('"');
	}
	
	public Deserializer(InputStream in) throws IOException {
		this(new InputStreamReader(in));
	}
	
	public void expect(char ch) throws SyntaxError, IOException {
		if (nextToken() != ch)
			throw new SyntaxError("expecting \""+ch+"\" got "+ttype);
	}
	
	public void expectWord(String str) throws SyntaxError, IOException {
		if (nextToken() != TT_WORD || !sval.equals(str))
			throw new SyntaxError("expecting \""+str+"\"");
		
	}
}
