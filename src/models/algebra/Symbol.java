package models.algebra;

public class Symbol {
	private String name;
	private String implName;
	private int arity = 0;			// -1: variable number
	private Type operatorType = Type.PREFIX;
	private Type implOperatorType = Type.PREFIX;
	private Symbol[] inverses = null;
	private models.algebra.Type[] signature = null;
	private int[] implParamOrder = null;
	private IImplGenerator generator = null;
	
	public Symbol(String name) {
		this.name = name;
		this.implName = name;
		this.arity = 0;
	}
	
	public Symbol(String name, int arity) {
		this.name = name;
		this.implName = name;
		this.arity = arity;
	}
	
	public Symbol(String name, int arity, Type operatorType) {
		this(name, arity);
		this.operatorType = operatorType;
		this.implOperatorType = operatorType;
	}
	
	public Symbol(String name, int arity, Type operatorType, String implName, Type implOperatorType) {
		this.name = name;
		this.implName = implName;
		this.arity = arity;
		this.operatorType = operatorType;
		this.implOperatorType = implOperatorType;
	}
	
	public Symbol(String name, int arity, Type operatorType, String implName, Type implOperatorType, int[] implParamOrder) {
		this.name = name;
		this.implName = implName;
		this.arity = arity;
		this.operatorType = operatorType;
		this.implOperatorType = implOperatorType;
		this.implParamOrder = implParamOrder;
	}
	
	public Symbol(String name, int arity, Type operatorType, IImplGenerator generator) {
		this.name = name;
		this.arity = arity;
		this.operatorType = operatorType;
		this.generator = generator;
		this.implOperatorType = Type.GENERATIVE;
	}
	
	public Symbol(String name, int arity, Type operatorType, IImplGenerator generator, boolean bSideEffect) {
		this.name = name;
		this.arity = arity;
		this.operatorType = operatorType;
		this.generator = generator;
		if (!bSideEffect) {
			this.implOperatorType = Type.GENERATIVE;
		} else {
			this.implOperatorType = Type.GENERATIVE_WITH_SIDE_EFFECT;
		}
	}
	
	public void setArity(int arity) {
		this.arity = arity;
	}

	public int getArity() {
		return arity;
	}

	public String getName() {
		return name;
	}

	public Type getOperatorType() {
		return operatorType;
	}
	
	public boolean isInfix() {
		return (operatorType == Type.INFIX);
	}
	
	public boolean isMethod() {
		return (operatorType == Type.METHOD || operatorType == Type.METHOD_WITH_SIDE_EFFECT);
	}

	public Symbol[] getInverses() {
		return inverses;
	}

	public void setInverses(Symbol[] inverses) {
		this.inverses = inverses;
	}

	public models.algebra.Type[] getSignature() {
		return signature;
	}

	public void setSignature(models.algebra.Type[] signature) {
		this.signature = signature;
	}

	public String getImplName() {
		return implName;
	}

	public void setImplName(String implName) {
		this.implName = implName;
	}

	public Type getImplOperatorType() {
		return implOperatorType;
	}
	
	public boolean isImplInfix() {
		return (implOperatorType == Type.INFIX);
	}
	
	public boolean isImplMethod() {
		return (implOperatorType == Type.METHOD || implOperatorType == Type.METHOD_WITH_SIDE_EFFECT);
	}
	
	public boolean isImplLambda() {
		return (implOperatorType == Type.LAMBDA || implOperatorType == Type.LAMBDA_WITH_SIDE_EFFECT);
	}
	
	public boolean isImplGenerative() {
		return (implOperatorType == Type.GENERATIVE || implOperatorType == Type.GENERATIVE_WITH_SIDE_EFFECT);
	}
	
	public boolean isImplWithSideEffect() {
		return (implOperatorType == Type.METHOD_WITH_SIDE_EFFECT 
				|| implOperatorType == Type.LAMBDA_WITH_SIDE_EFFECT 
				|| implOperatorType == Type.GENERATIVE_WITH_SIDE_EFFECT);
	}

	public void setImplOperatorType(Type implOperatorType) {
		this.implOperatorType = implOperatorType;
	}
	
	public int[] getImplParamOrder() {
		return implParamOrder;
	}
	
	public void setGenerator(IImplGenerator generator) {
		this.generator = generator; 
	}
	
	/**
	 * Generate the implementation of this symbol
	 * @param type the type of this symbol
	 * @param childrenImpl the implementations of the children
	 * @param childrenSideEffects (input) an array of the side effects of the children
	 * @param sideEffect (output) an array of the side effect of this symbol
	 * @return the implementation
	 */
	public String generate(models.algebra.Type type, String[] childrenImpl, String[] childrenSideEffects, String[] sideEffect) {
		if (generator != null) {
			return generator.generate(type, childrenImpl, childrenSideEffects, sideEffect);
		}
		return null;
	}

	public boolean equals(Object another) {
		if (!(another instanceof Symbol)) return false;
		return name.equals(((Symbol) another).name) && arity == ((Symbol) another).arity;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	public String toString() {
		return name;
	}
	
	public String toImplementation() {
		return implName;
	}
	
	public enum Type {
		PREFIX,
		INFIX,
		METHOD,
		METHOD_WITH_SIDE_EFFECT,
		LAMBDA,
		LAMBDA_WITH_SIDE_EFFECT,
		GENERATIVE,
		GENERATIVE_WITH_SIDE_EFFECT
	}
	
	public Memento createMemento() {
		return new Memento(implName, implOperatorType);
	}
	
	public void setMemento(Memento memento) {
		this.implName = memento.implName;
		this.implOperatorType = memento.implOperatorType;
	}
	
	public static class Memento {
		private String implName;
		private Type implOperatorType = Type.PREFIX;
		
		public Memento(String implName, Type implOperatorType) {
			this.implName = implName;
			this.implOperatorType = implOperatorType;
		}
	}
	
	public interface IImplGenerator {
		/**
		 * Generate the implementation
		 * @param type the type of this expression
		 * @param children the implementations of the children
		 * @param childrenSideEffects (input) an array of the side effects of the children
		 * @param sideEffect (output) an array of the side effect of this generator
		 * @return the generated implementation
		 */
		public String generate(models.algebra.Type type, String children[], String[] childrenSideEffects, String[] sideEffect);
	}
}
