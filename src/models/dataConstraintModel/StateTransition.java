package models.dataConstraintModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import models.algebra.Expression;
import models.algebra.InvalidMessage;
import models.algebra.Position;
import models.algebra.Term;
import models.algebra.ValueUndefined;
import models.algebra.Variable;
import models.dataFlowModel.ResolvingMultipleDefinitionIsFutureWork;

public class StateTransition {
	private Expression curStateExpression = null;
	private Expression nextStateExpression = null;
	private Expression messageExpression = null;
	
	public Expression getCurStateExpression() {
		return curStateExpression;
	}
	
	public void setCurStateExpression(Expression curStateExpression) {
		this.curStateExpression = curStateExpression;
	}
	
	public Expression getNextStateExpression() {
		return nextStateExpression;
	}
	
	public void setNextStateExpression(Expression nextStateExpression) {
		this.nextStateExpression = nextStateExpression;
	}

	public Expression getMessageExpression() {
		return messageExpression;
	}

	public void setMessageExpression(Expression messageExpression) {
		this.messageExpression = messageExpression;
	}
	
	public boolean isRightUnary() {
		for (Position pos : curStateExpression.getVariables().keySet()) {
			if (nextStateExpression.contains(curStateExpression.getVariables().get(pos))) return false;
		}
		return true;
	}

	public Expression deriveMessageConstraintFor(Expression curStateValue, Expression nextStateValue) throws InvalidMessage, ResolvingMultipleDefinitionIsFutureWork {
		HashMap<Variable, ArrayList<Expression>> bindings = new HashMap<>();
		
		Expression curStateTerm = getCurStateExpression();
		HashMap<Position, Variable> curStateVars = curStateTerm.getVariables();		
		for (Entry<Position, Variable> curStateVarEnt: curStateVars.entrySet()) {
			Variable var = curStateVarEnt.getValue();
			Position varPos = curStateVarEnt.getKey();
			Expression valueCalc = curStateTerm.getInverseMap(curStateValue, varPos);
			if (valueCalc != null) {
				ArrayList<Expression> values = bindings.get(var);
				if (values == null) {
					values = new ArrayList<Expression>();
					bindings.put(var, values);
				}
				values.add(valueCalc);
			}
		}
		
		Expression nextStateTerm = (Expression) getNextStateExpression().clone();
		for (Variable var: bindings.keySet()) {
			HashMap<Position, Variable> vars2 = nextStateTerm.getVariables();
			for (Variable var2: vars2.values()) {
				if (var.equals(var2) && bindings.get(var).size() == 1) {
					if (nextStateTerm instanceof Term) {
						nextStateTerm = ((Term) nextStateTerm).substitute(var, bindings.get(var).get(0));
					} else if (nextStateTerm instanceof Variable && nextStateTerm.equals(var)) {
						nextStateTerm = bindings.get(var).get(0);
					}
				}
			}
		}
		
		HashMap<Position, Variable> nextStateVars = nextStateTerm.getVariables();
		for (Entry<Position, Variable> nextStateVarEnt: nextStateVars.entrySet()) {
			Variable var = nextStateVarEnt.getValue();
			Position varPos = nextStateVarEnt.getKey();
			Expression valueCalc = nextStateTerm.getInverseMap(nextStateValue, varPos);
			if (valueCalc != null) {
				ArrayList<Expression> values = bindings.get(var);
				if (values == null) {
					values = new ArrayList<Expression>();
					bindings.put(var, values);
				}
				values.add(valueCalc);
			}
		}
		
		Expression messageTerm = getMessageExpression();
		if (!(messageTerm instanceof Term)) throw new InvalidMessage();
		HashMap<Position, Variable> messageVars = messageTerm.getVariables();
		for (Variable var: messageVars.values()) {
			if (bindings.get(var) != null) {
				if (bindings.get(var).size() > 1) throw new ResolvingMultipleDefinitionIsFutureWork();
				messageTerm = ((Term) messageTerm).substitute(var, bindings.get(var).iterator().next());
			}
		}
		return messageTerm;
	}

	public Expression deriveMessageConstraintFor(Expression curStateValue) throws InvalidMessage, ResolvingMultipleDefinitionIsFutureWork {
		HashMap<Variable, ArrayList<Expression>> bindings = new HashMap<>();
		
		Expression curStateTerm = getCurStateExpression();
		HashMap<Position, Variable> curStateVars = curStateTerm.getVariables();		
		for (Entry<Position, Variable> curStateVarEnt: curStateVars.entrySet()) {
			Variable var = curStateVarEnt.getValue();
			Position varPos = curStateVarEnt.getKey();
			Expression valueCalc = curStateTerm.getInverseMap(curStateValue, varPos);
			if (valueCalc != null) {
				ArrayList<Expression> values = bindings.get(var);
				if (values == null) {
					values = new ArrayList<Expression>();
					bindings.put(var, values);
				}
				values.add(valueCalc);
			}
		}
		
		Expression messageTerm = getMessageExpression();
		if (!(messageTerm instanceof Term)) throw new InvalidMessage();
		HashMap<Position, Variable> messageVars = messageTerm.getVariables();
		for (Variable var: messageVars.values()) {
			if (bindings.get(var) != null) {
				if (bindings.get(var).size() > 1) throw new ResolvingMultipleDefinitionIsFutureWork();
				messageTerm = ((Term) messageTerm).substitute(var, bindings.get(var).iterator().next());
			}
		}
		return messageTerm;
	}

	public Expression deriveNextStateExpressionFor(Expression curStateValue, Term concreteMessage) 
			throws ResolvingMultipleDefinitionIsFutureWork, ValueUndefined {
		HashMap<Variable, Expression> bindings = new HashMap<>();

		Expression curStateTerm = getCurStateExpression();
		HashMap<Position, Variable> curStateVars = curStateTerm.getVariables();
		for (Entry<Position, Variable> curStateVarEnt: curStateVars.entrySet()) {
			Variable var = curStateVarEnt.getValue();
			Position varPos = curStateVarEnt.getKey();
			Expression valueCalc = curStateTerm.getInverseMap(curStateValue, varPos);
			if (valueCalc != null) {
				if (bindings.get(var) != null) throw new ResolvingMultipleDefinitionIsFutureWork();
				bindings.put(var, valueCalc);
			}
		}
		
		Expression messageTerm = getMessageExpression();
		HashMap<Position, Variable> messageVars = messageTerm.getVariables();
		if (concreteMessage != null) {
			for (Entry<Position, Variable> messageVarEnt: messageVars.entrySet()) {
				Variable var = messageVarEnt.getValue();
				Position varPos = messageVarEnt.getKey();
				Expression valueCalc = concreteMessage.getSubTerm(varPos);
				if (valueCalc != null) {
					if (bindings.get(var) != null) throw new ResolvingMultipleDefinitionIsFutureWork();
					bindings.put(var, valueCalc);
				}
			}
		}
		
		Expression nextStateTerm = getNextStateExpression();
		if (nextStateTerm instanceof Variable) {
			nextStateTerm = bindings.get((Variable) nextStateTerm);
			if (nextStateTerm == null) throw new ValueUndefined();
		} else {
			for (Variable var: bindings.keySet()) {
				nextStateTerm = ((Term) nextStateTerm).substitute(var, bindings.get(var));
			}
		}
		return nextStateTerm;
	}
}
