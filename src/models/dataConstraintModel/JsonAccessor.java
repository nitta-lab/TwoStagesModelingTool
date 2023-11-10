package models.dataConstraintModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.algebra.Constant;
import models.algebra.Expression;
import models.algebra.LambdaAbstraction;
import models.algebra.Position;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Type;
import models.algebra.Variable;

public class JsonAccessor extends Term {

	public JsonAccessor(Symbol symbol) {
		super(symbol);
	}
	
	public Type getType() {
		if (symbol.equals(DataConstraintModel.dotParam)) {
			Type valueType = null;
			if (getChild(1) instanceof Term) {
				valueType = ((Term) getChild(1)).getType();
			} else if (getChild(1) instanceof Variable) {
				valueType = ((Variable) getChild(1)).getType();
			}
			if (valueType != null) return valueType;
		}
		return super.getType();
	}
	
	@Override
	public Expression reduce() {
		Expression reducedTerm = super.reduce();
		if (reducedTerm instanceof Term) {
			if (symbol.equals(DataConstraintModel.dot) && getChildren().size() >= 2) {
				// this term is `json.key`.
				Expression expJson = getChild(0);
				Expression expKey = getChild(1);
				if (expKey instanceof Constant && expJson instanceof Term) {
					reducedTerm = getValue((Term) expJson, (Constant) expKey);
				}
			}
		}
		return reducedTerm;
	}
	
	private Expression getValue(Term json, Constant key) {
		if (!json.getSymbol().equals(DataConstraintModel.addMember)) return null;
		if (json.getChild(1).equals(key)) {
			return json.getChild(2);
		}
		if (json.getChild(0) == null || json.getChild(0).equals(DataConstraintModel.nil)) return null;
		return getValue((Term) json.getChild(0), key);
	}

	@Override
	public Expression getInverseMap(Expression outputValue, Position targetPos) {
		if (targetPos.isEmpty()) return outputValue;
		targetPos = (Position) targetPos.clone();
		int i = targetPos.removeHeadOrder();
		Symbol[] inverseSymbols = symbol.getInverses();
		if (i == 0) {
			if (symbol.equals(DataConstraintModel.dot) && getChildren().size() >= 2) {
				// this term is `json.key`.
				Expression expJson = getChild(0);
				Expression expKey = getChild(1);
				JsonType jsonType = null;
				if (expJson instanceof Variable) {
					jsonType = (JsonType) ((Variable) expJson).getType();
				} else if (expJson instanceof Term) {
					jsonType = (JsonType) ((Term) expJson).getType();
				}
				String keyName = null;
				if (expKey instanceof Constant) {
					keyName = ((Constant) expKey).getSymbol().getName();
					Term jsonTerm = new Constant(DataConstraintModel.nil);
					jsonTerm.setType(DataConstraintModel.typeJson);
					int v = 1;
					Map<String, Variable> vars = new HashMap<>();
					Set<String> keySet = new HashSet<>();
					if (jsonType == null || jsonType == DataConstraintModel.typeJson) {
						keySet.add(keyName);
					} else {
						keySet.addAll(jsonType.getKeys());
					}
					for (String key: keySet) {
						Term addMemberTerm = new Term(DataConstraintModel.addMember);	// addMember(jsonTerm, key, v)
						addMemberTerm.addChild(jsonTerm);
						addMemberTerm.addChild(new Constant(key));
						Variable var = new Variable("v" + v);
						addMemberTerm.addChild(var);
						vars.put(key, var);
						jsonTerm = addMemberTerm;
						v++;
					}
					Variable var = vars.get(keyName);
					LambdaAbstraction lambdaAbstraction = new LambdaAbstraction(var, jsonTerm);	// v -> addMember(jsonTerm, key, v)
					inverseSymbols = new Symbol[] { lambdaAbstraction };
				}
			} else if (symbol.equals(DataConstraintModel.dotParam) && getChildren().size() >= 2) {
				// this term is `json.{param}`.
				Expression expListOrMap = getChild(0);
				Expression expKey = getChild(1);
				JsonType jsonType = null;
				if (expListOrMap instanceof Variable) {
					jsonType = (JsonType) ((Variable) expListOrMap).getType();
				} else if (expListOrMap instanceof Term) {
					jsonType = (JsonType) ((Term) expListOrMap).getType();
				}
				Type keyType = null;
				if (expKey instanceof Variable) {
					keyType = (JsonType) ((Variable) expKey).getType();
				} else if (expKey instanceof Term) {
					keyType = (JsonType) ((Term) expKey).getType();
				}
				if (jsonType != null && keyType != null) { 
					if (DataConstraintModel.typeList.isAncestorOf(jsonType) || keyType.equals(DataConstraintModel.typeInt)) {
						Term setElementTerm = new Term(DataConstraintModel.set);			// set(list, idx, v)
						setElementTerm.addChild(new Constant(DataConstraintModel.nil));
						setElementTerm.addChild(expKey);
						Variable var = new Variable("v");
						setElementTerm.addChild(var);
						LambdaAbstraction lambdaAbstraction = new LambdaAbstraction(var, setElementTerm);	// v -> set(list, idx, v)
						inverseSymbols = new Symbol[] { lambdaAbstraction };
					} else if (DataConstraintModel.typeMap.isAncestorOf(jsonType) || keyType.equals(DataConstraintModel.typeString)) {
						Term insertEntryTerm = new Term(DataConstraintModel.insert);			// insert(map, key, v)
						insertEntryTerm.addChild(new Constant(DataConstraintModel.nil));
						insertEntryTerm.addChild(expKey);
						Variable var = new Variable("v");
						insertEntryTerm.addChild(var);
						LambdaAbstraction lambdaAbstraction = new LambdaAbstraction(var, insertEntryTerm);	// v -> insert(map, key, v)
						inverseSymbols = new Symbol[] { lambdaAbstraction };
					}
				}
			}
		}
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
	
	public String toString() {
		if (symbol.equals(DataConstraintModel.dotParam)) {
			return children.get(0).toString() + symbol.toString() + "{" + children.get(1).toString() + "}";		
		}
		return super.toString();
	}
	
	public String toImplementation(String[] sideEffects) {
		if (symbol.equals(DataConstraintModel.dotParam)) {
			return children.get(0).toImplementation(sideEffects) + symbol.toImplementation() + "{" + children.get(1).toImplementation(sideEffects) + "}";		
		}
		return super.toImplementation(sideEffects);
	}
	
	@Override
	public Object clone() {
		JsonAccessor newTerm = new JsonAccessor(symbol);
		for (Expression e: children) {
			newTerm.addChild((Expression) e.clone());
		}
		newTerm.type = type;
		return newTerm;
	}
}
