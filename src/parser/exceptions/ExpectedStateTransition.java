package parser.exceptions;

public class ExpectedStateTransition extends ParseException {

	public ExpectedStateTransition(int line) {
		super(line);
	}

}
