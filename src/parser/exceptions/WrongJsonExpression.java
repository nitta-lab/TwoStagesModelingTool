package parser.exceptions;

public class WrongJsonExpression extends ParseException {

	public WrongJsonExpression(int line) {
		super(line);
	}

}
