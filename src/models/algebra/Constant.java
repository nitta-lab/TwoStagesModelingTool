package models.algebra;

import java.util.ArrayList;

public class Constant extends Term {

	public Constant(String value) {
		super(new Symbol(value, 0), new ArrayList<Expression>());
	}
	
	public Constant(String value, Type type) {
		super(new Symbol(value, 0), new ArrayList<Expression>());
		symbol.setSignature(new Type[] {type});
	}
	
	public Constant(Symbol symbol) {
		super(symbol);
	}
	
	@Override
	public boolean equals(Object another) {
		if (!(another instanceof Constant)) return false;
		return symbol.equals(((Constant) another).symbol);
	}
	
	@Override
	public Object clone() {
		Constant c =  new Constant(symbol);
		c.setType(type);
		return c;
	}
	
	public String toString() {
		return symbol.getName();
	}
	
	public String toImplementation(String[] sideEffects) {
		if (symbol.isImplGenerative()) {
			String exp = symbol.generate(getType(), new String[] {}, new String[] {}, sideEffects);
			return exp;
		}
		return symbol.getImplName();
	}
}
