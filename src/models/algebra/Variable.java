package models.algebra;

import java.util.HashMap;

public class Variable extends Expression {
	private String name;
	private Type type = null;
	
	public Variable(String name) {
		super();
		this.name = name;
	}

	public Variable(String name, Type type) {
		super();
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}
	
	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public <T extends Expression> HashMap<Position, T> getSubTerms(Class<T> clazz) {
		HashMap<Position, T> subTerms = new HashMap<>();
		if (clazz == this.getClass()) {
			subTerms.put(new Position(), (T) this);
		}
		return subTerms;
	}
	
	@Override
	public Expression getSubTerm(Position pos) {
		if (pos.isEmpty()) return this;
		return null;
	}
	
	@Override
	public Expression unify(Expression another) {
		return (Expression) another.clone();
	}

	@Override
	public Expression getInverseMap(Expression outputValue, Position targetPos) {
		if (targetPos.isEmpty()) return outputValue;
		return null;
	}

	@Override
	public boolean contains(Expression exp) {
		return equals(exp);
	}

	@Override
	public boolean equals(Object another) {
		if (!(another instanceof Variable)) return false;
		return name.equals(((Variable) another).name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public Object clone() {
		return new Variable(name, type);
	}
	
	public String toString() {
		if (type == null) return name;
		return name + ":" + type.getTypeName();
	}
	
	public String toImplementation(String[] sideEffects) {
		return name;
	}
}
