package models.algebra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class Term extends Expression {
	protected Symbol symbol = null;
	protected List<Expression> children = new ArrayList<>();
	protected Type type = null;
	
	public Term(Symbol symbol) {
		super();
		this.symbol = symbol;
	}

	public Term(Symbol symbol, List<Expression> children) {
		super();
		this.symbol = symbol;
		this.children = children;
	}

	public Term(Symbol symbol, Expression[] children) {
		super();
		this.symbol = symbol;
		this.children = new ArrayList<>(Arrays.asList(children));
	}

	public Symbol getSymbol() {
		return symbol;
	}
	
	public int getArity() {
		return symbol.getArity();
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public Type getType()  {
		if (type == null) {
			if (symbol.getSignature() == null) return null;
			return symbol.getSignature()[0];
		}
		return type;
	}
		
	public boolean addChild(Expression child) {
		if (getArity() != -1 && children.size() >= getArity()) return false;
		children.add(child);
		return true;
	}
		
	public void addChild(Expression child, boolean bForced) {
		if (!bForced && getArity() != -1 && children.size() >= getArity()) return;
		children.add(child);
	}
	
	public Expression getChild(int n) {
		return children.get(n);
	}
	
	public List<Expression> getChildren() {
		return children;
	}
	
	public <T extends Expression> HashMap<Position, T> getSubTerms(Class<T> clazz) {
		HashMap<Position, T> subTerms = new HashMap<>();
		if (clazz == this.getClass()) {
			subTerms.put(new Position(), (T) this);
		}
		for (int i = 0; i < children.size(); i++) {
			HashMap<Position, T> terms = children.get(i).getSubTerms(clazz);
			for (Entry<Position, T> term: terms.entrySet()) {
				Position pos = term.getKey();
				pos.addHeadOrder(i);
				subTerms.put(pos, term.getValue());
			}
		}
		return subTerms;
	}
	
	public Expression getSubTerm(Position pos) {
		if (pos.isEmpty()) return this;
		pos = (Position) pos.clone();
		int i = pos.removeHeadOrder();
		if (i >= children.size()) return null;
		return children.get(i).getSubTerm(pos);
	}
	
	public Term substitute(Variable variable, Expression value) {
		Term newTerm = (Term) this.clone();
		HashMap<Position, Variable> variables = getVariables();
		for (Entry<Position, Variable> varEnt: variables.entrySet()) {
			if (varEnt.getValue().equals(variable)) {
				newTerm.replaceSubTerm(varEnt.getKey(), value);
			}
		}
		return newTerm;
	}
	
	public void replaceSubTerm(Position pos, Expression newSubTerm) {
		if (pos.isEmpty()) return;
		pos = (Position) pos.clone();
		int i = pos.removeHeadOrder();
		if (pos.isEmpty()) {
			children.set(i, newSubTerm);
		} else {
			if (!(children.get(i) instanceof Term)) return;
			((Term) children.get(i)).replaceSubTerm(pos, newSubTerm);
		}
	}
	
	@Override
	public Expression unify(Expression another) {
		if (another instanceof Variable) return (Expression) this.clone();
		if (another instanceof Term) {
			Term anotherTerm = (Term) another;
			if (!symbol.equals(anotherTerm.symbol)) return null;
			if (children.size() != anotherTerm.children.size()) return null;
			Term unifiedTerm = new Term(symbol);
			for (int i = 0; i < children.size(); i++) {
				unifiedTerm.addChild(children.get(i).unify(anotherTerm.children.get(i)));
			}
			return unifiedTerm;
		} else {
			return null;
		}		
	}
	
	public Expression reduce() {
		if (symbol.isLambda()) {
			// Lambda beta-reduction
			LambdaAbstraction newSymbol = ((LambdaAbstraction) symbol);
			Term newTerm = newSymbol.getTerm();
			List<Variable> newVariables = newSymbol.getVariables();
			List<Expression> newChildren = children;
			while (newVariables.size() > 0 && newChildren.size() > 0) {
				newTerm = newTerm.substitute(newVariables.get(0), newChildren.get(0));
				newVariables = newVariables.subList(1, newVariables.size());
				newChildren = newChildren.subList(1, newChildren.size());
				newSymbol = new LambdaAbstraction(newVariables, newTerm);
			}
			if (newSymbol.arity == 0 && newChildren.size() == 0) {
				return newTerm;
			} else {
				return new Term(newSymbol, newChildren);
			}
		} else {
			// Calculate inverse map
			List<Expression> newChildren = new ArrayList<>();
			boolean bReduced = false;
			for (Expression child: children) {
				if (child instanceof Term && !(child instanceof Constant)) {
					child = ((Term) (child)).reduce();
					bReduced = true;
				}
				newChildren.add(child);
			}
			if (symbol.arity == 1 && newChildren.size() == 1) {
				Expression child = newChildren.get(0);
				if (child instanceof Term && !(child instanceof Constant)) {
					Symbol childSymbol = ((Term) child).getSymbol();
					if (childSymbol.getInverses() != null) {
						for (int i = 0; i < childSymbol.getInverses().length; i++) {
							if (symbol.equals(childSymbol.getInverses()[i])) {
								return ((Term) child).getChild(i);
							}
						}
					}
				}
			}
			if (!bReduced) return this;
			Term newTerm = new Term(symbol, newChildren);
			newTerm.setType(type);
			return newTerm;
		}
	}
	
	@Override
	public Expression getInverseMap(Expression outputValue, Position targetPos) {
		if (targetPos.isEmpty()) return outputValue;
		targetPos = (Position) targetPos.clone();
		int i = targetPos.removeHeadOrder();
		Symbol[] inverseSymbols = symbol.getInverses();
		if (inverseSymbols == null || i >= inverseSymbols.length || inverseSymbols[i] == null) return null;
		Term inverseMap = new Term(inverseSymbols[i]);
		inverseMap.addChild(outputValue);
		for (int n = 0; n < inverseSymbols[i].getArity(); n++) {
			if (n != i) {
				inverseMap.addChild(children.get(n));
			}
		}
		return children.get(i).getInverseMap(inverseMap, targetPos);
	}
	
	@Override
	public boolean contains(Expression exp) {
		if (equals(exp)) return true;
		for (Expression e: children) {
			if (e.contains(exp)) return true;
		}
		return false;
	}
	
	@Override
	public boolean equals(Object another) {
		if (!(another instanceof Term)) return false;
		if (this == another) return true;
		Term anotherTerm = (Term) another;
		if (!symbol.equals(anotherTerm.symbol)) return false;
		if (children.size() != anotherTerm.children.size()) return false;
		if (type != anotherTerm.type) return false;
		for (int i = 0; i < children.size(); i++) {
			Expression e = children.get(i);
			Expression e2 = anotherTerm.children.get(i);
			if (!e.equals(e2)) return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return symbol.hashCode();
	}
	
	
	@Override
	public Object clone() {
		Term newTerm = new Term(symbol);
		for (Expression e: children) {
			newTerm.addChild((Expression) e.clone());
		}
		newTerm.type = type;
		return newTerm;
	}
	
	public String toString() {
		if (getArity() == 2 && symbol.isInfix()) {
			return "(" + children.get(0) + symbol.toString() + children.get(1) + ")";
		}
		if (getArity() >= 1 && symbol.isMethod()) {
			String exp = children.get(0).toString() + "." + symbol.toString() + "(";
			String delimiter = "";
			for (int i = 1; i < children.size(); i++) {
				Expression e = children.get(i);
				exp += (delimiter + e.toString());
				delimiter = ",";
			}
			return exp + ")";
		} else {
			String exp = symbol.toString() + "(";
			String delimiter = "";
			for (Expression e: children) {
				exp += (delimiter + e.toString());
				delimiter = ",";
			}
			return exp + ")";
		}
	}

	
	public String toImplementation(String[] sideEffects) {
		int[] implParamOrder = symbol.getImplParamOrder();
		if (symbol.isImplLambda()) {
			String[] components = symbol.getImplName().split("->");
			String component0 = components[0].replace("(", "").replace(")", "");
			String[] params = component0.split(",");
			String exp = components[1];
			if (implParamOrder == null) {
				for (int i = 0; i < params.length; i++) {
					exp = exp.replace(params[i], children.get(i).toImplementation(sideEffects));
				}
			} else {
				for (int i = 0; i < params.length; i++) {
					exp = exp.replace(params[i], children.get(implParamOrder[i]).toImplementation(sideEffects));
				}
			}
			if (symbol.isImplWithSideEffect()) {
				sideEffects[0] = sideEffects[0] + exp + ";\n";
				if (implParamOrder == null) {
					exp = children.get(0).toImplementation(new String[] {""});
				} else {
					exp = children.get(implParamOrder[0]).toImplementation(new String[] {""});
				}
			}
			return exp;
		}
		if (symbol.isImplGenerative()) {
			String childrenImpl[] = new String[children.size()];
			String childrenSideEffects[] = new String[children.size()];
			if (implParamOrder == null) {
				for (int i = 0; i < children.size(); i++) {
					String childSideEffect[] = new String[] {""};
					childrenImpl[i] = children.get(i).toImplementation(childSideEffect);
					childrenSideEffects[i] = childSideEffect[0];
				}
				String exp = symbol.generate(getType(), childrenImpl, childrenSideEffects, sideEffects);
				if (symbol.isImplWithSideEffect()) {
					sideEffects[0] = sideEffects[0] + exp;
					exp = children.get(0).toImplementation(new String[] {""});	// the value of this term
				}
				return exp;
			} else {
				for (int i = 0; i < children.size(); i++) {
					String childSideEffect[] = new String[] {""};
					childrenImpl[i] = children.get(implParamOrder[i]).toImplementation(childSideEffect);
					childrenSideEffects[i] = childSideEffect[0];
				}
				String exp = symbol.generate(getType(), childrenImpl, childrenSideEffects, sideEffects);
				if (symbol.isImplWithSideEffect()) {
					sideEffects[0] = sideEffects[0] + exp;
					exp = children.get(implParamOrder[0]).toImplementation(new String[] {""});	// the value of this term
				}
				return exp;
			}
		}
		if (getArity() == 2 && symbol.isImplInfix()) {
			if (implParamOrder == null) {
				return "(" + children.get(0).toImplementation(sideEffects) + symbol.toImplementation() + children.get(1).toImplementation(sideEffects) + ")";
			} else {
				return "(" + children.get(implParamOrder[0]).toImplementation(sideEffects) + symbol.toImplementation() + children.get(implParamOrder[1]).toImplementation(sideEffects) + ")";				
			}
		}
		if (getArity() >= 1 && symbol.isImplMethod()) {
			if (implParamOrder == null) {
				String exp = children.get(0).toImplementation(sideEffects) + "." + symbol.toImplementation() + "(";
				String delimiter = "";
				for (int i = 1; i < children.size(); i++) {
					Expression e = children.get(i);
					exp += (delimiter + e.toImplementation(sideEffects));
					delimiter = ",";
				}
				exp += ")";
				if (symbol.isImplWithSideEffect()) {
					sideEffects[0] = sideEffects[0] + exp + ";\n";
					exp = children.get(0).toImplementation(new String[] {""});
				}
				return exp;
			} else {
				String exp = children.get(implParamOrder[0]).toImplementation(sideEffects) + "." + symbol.toImplementation() + "(";
				String delimiter = "";
				for (int i = 1; i < children.size(); i++) {
					Expression e = children.get(implParamOrder[i]);
					exp += (delimiter + e.toImplementation(sideEffects));
					delimiter = ",";
				}
				exp += ")";
				if (symbol.isImplWithSideEffect()) {
					sideEffects[0] = sideEffects[0] + exp + ";\n";
					exp = children.get(implParamOrder[0]).toImplementation(new String[] {""});
				}
				return exp;
			}
		} else {
			if (implParamOrder == null) {
				String exp = symbol.toImplementation() + "(";
				String delimiter = "";
				for (Expression e: children) {
					exp += (delimiter + e.toImplementation(sideEffects));
					delimiter = ",";
				}
				return exp + ")";
			} else {
				String exp = symbol.toImplementation() + "(";
				String delimiter = "";
				for (int i = 0; i < children.size(); i++) {
					Expression e = children.get(implParamOrder[i]);
					exp += (delimiter + e.toImplementation(sideEffects));
					delimiter = ",";
				}
				return exp + ")";
			}
		}
	}
}
