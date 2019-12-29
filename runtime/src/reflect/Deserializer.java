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
