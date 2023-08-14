package models.algebra;

import java.util.HashMap;

public abstract class Expression implements Cloneable {
	public abstract Expression getSubTerm(Position pos);
	/**
	 * Get the unification between this expression and another expression.
	 * @param another another expression
	 * @return unified expression
	 */
	public abstract Expression unify(Expression another);
	/**
	 * Get the inverse map to obtain a sub-term of a given output value back from the output value itself.
	 * @param outputValue an output value (usually a term)
	 * @param targetPos a position in outputValue
	 * @return inverse map
	 */
	public abstract Expression getInverseMap(Expression outputValue, Position targetPos);
	public abstract boolean contains(Expression exp);
	public abstract Object clone();
	public abstract <T extends Expression> HashMap<Position, T> getSubTerms(Class<T> clazz);
	
	public HashMap<Position, Variable> getVariables() {
		return getSubTerms(Variable.class);
	}
	
	/**
	 * Get the implementation of this expression.
	 * @param sideEffects an array with an optional implementation that should be written before the evaluation of this expression
	 * @return the implementation to represent the value of this expression
	 */
	public String toImplementation(String[] sideEffects) {
		return toString();
	}
}
