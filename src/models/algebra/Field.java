package models.algebra;

import java.util.ArrayList;

/**
 * A field in the implementation (regarded as a constant in the algebraic system)
 * @author Nitta
 *
 */
public class Field extends Constant {

	public Field(String name) {
		super(name);
	}
	
	public Field(String name, Type type) {
		super(name, type);
	}
	
	public Field(Symbol symbol) {
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
		if (!(another instanceof Field)) return false;
		return symbol.equals(((Field) another).symbol);
	}
	
	@Override
	public Object clone() {
		return new Field(symbol);
	}
	
	public String toImplementation(String[] sideEffects) {
		return "this." + super.toImplementation(sideEffects);
	}
}
