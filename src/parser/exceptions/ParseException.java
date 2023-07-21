package parser.exceptions;

public class ParseException extends Exception {
	protected int line;
	
	public ParseException(int line) {
		super("at line " + (line + 1));
		this.line = line;
	}
}
