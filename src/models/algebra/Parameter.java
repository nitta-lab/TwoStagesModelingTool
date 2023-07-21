package models.algebra;

/**
 * A parameter in the implementation (regarded as a constant in the algebraic system)
 * @author Nitta
 *
 */
public class Parameter extends Constant {

	public Parameter(String name) {
		super(name);
	}
	
	public Parameter(String name, Type type) {
		super(name, type);
	}
	
	public Parameter(Symbol symbol) {
		super(symbol);
	}
	
	public Type getType() {
		if (symbol.getSignature().length >= 1) {
			return symbol.getSignature()[0];
		}
		return null;
	}
	
	@Override
	public boolean equals(Object another) {
		if (!(another instanceof Parameter)) return false;
		return symbol.equals(((Parameter) another).symbol);
	}
	
	@Override
	public Object clone() {
		return new Parameter(symbol);
	}
}
