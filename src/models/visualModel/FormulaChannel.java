package models.visualModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import models.algebra.Expression;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Variable;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.ResourcePath;
import models.dataConstraintModel.StateTransition;
import models.dataFlowModel.DataTransferChannel;

public class FormulaChannel extends DataTransferChannel {
	private Symbol defaultOperator = null;
	private String formula = null;
	private Expression formulaRhs = null;

	public FormulaChannel(String channelName, Symbol defaultOperator) {
		super(channelName);
		this.defaultOperator = defaultOperator;
	}
	
	public void addChannelMemberAsInput(ChannelMember channelMember) {
//		StateTransition st = new StateTransition();
//		st.setCurStateExpression(new Variable(channelMember.getIdentifierTemplate().getResourceName() + "1"));
//		st.setNextStateExpression(new Variable(channelMember.getIdentifierTemplate().getResourceName() + "2"));
//		channelMember.setStateTransition(st);
		super.addChannelMemberAsInput(channelMember);
		if (formula != null && getInputChannelMembers().size() > 1) {
			formula += " " + defaultOperator + " " + channelMember.getResource().getResourceName();
			if (formulaRhs != null) {
				if (formulaRhs instanceof Variable) {
					Term newTerm = new Term(defaultOperator);
					newTerm.addChild(formulaRhs);
					newTerm.addChild(new Variable(channelMember.getResource().getResourceName()), true);
					formulaRhs = newTerm;
				} else if (formulaRhs instanceof Term) {
					Term newTerm = new Term(defaultOperator);
					newTerm.addChild(formulaRhs);
					newTerm.addChild(new Variable(channelMember.getResource().getResourceName()));
					formulaRhs = newTerm;						
				}
			}
		} else {
			if (formula == null) formula = "";
			formula += channelMember.getResource().getResourceName();
			formulaRhs = new Variable(channelMember.getResource().getResourceName());
		}
		if (formulaRhs != null) {
			setFormulaTerm(formulaRhs);
		}
	}

	public void addChannelMemberAsOutput(ChannelMember channelMember) {
//		StateTransition st = new StateTransition();
//		st.setCurStateExpression(new Variable(channelMember.getIdentifierTemplate().getResourceName() + "1"));
//		channelMember.setStateTransition(st);
		super.addChannelMemberAsOutput(channelMember);
		if (getOutputChannelMembers().size() == 1) {
			if (formula == null) formula = "";
			if (!formula.contains("==")) {
				formula = channelMember.getResource().getResourceName() + " == " + formula;
			}
		}
		if (formulaRhs != null) {
			setFormulaTerm(formulaRhs);				
		}
	}

	public Symbol getDefaultOperator() {
		return defaultOperator;
	}

	public void setDefaultOperator(Symbol defaultOperator) {
		this.defaultOperator = defaultOperator;
	}
	
	public void setFormulaTerm(Expression rhs) {
		formulaRhs = rhs;
		Collection<Variable> variables;
		if (rhs instanceof Variable) {
			variables = new ArrayList<>();
			variables.add((Variable) rhs);
		} else if (rhs instanceof Term) {
			variables = ((Term) rhs).getVariables().values();
		} else {
			return;
		}
		Map<ResourcePath, Variable> curStates = new HashMap<>();
		Map<ResourcePath, Variable> nextStates = new HashMap<>();
		Map<Variable, Variable> resToNextVar = new HashMap<>();
		for (ChannelMember cm: this.getInputChannelMembers()) {
			ResourcePath id = cm.getResource();
			String resName = id.getResourceName();
			Variable curVar = new Variable(resName + "1");
			Variable nextVar = new Variable(resName + "2");
			curStates.put(id, curVar);
			nextStates.put(id, nextVar);
			for (Variable var: variables) {
				if (var.getName().equals(resName)) {
					resToNextVar.put(var, nextVar);
					break;
				}
			}
		}
		Symbol update = new Symbol("update");
		update.setArity(resToNextVar.keySet().size());
		for (ChannelMember cm: getInputChannelMembers()) {
			ResourcePath id = cm.getResource();
			StateTransition st = new StateTransition();
			st.setCurStateExpression(curStates.get(id));
			st.setNextStateExpression(nextStates.get(id));
			Term message = new Term(update);
			for (Variable var: resToNextVar.values()) {
				message.addChild(var);
			}
			st.setMessageExpression(message);
			cm.setStateTransition(st);
		}
		
		if (rhs instanceof Variable) {
			rhs = resToNextVar.get((Variable) rhs);
		} else if (rhs instanceof Term) {
			formulaRhs = rhs;
			for (Variable var: resToNextVar.keySet()) {
				rhs = ((Term) rhs).substitute(var, resToNextVar.get(var));
			}
		}
		for (ChannelMember cm: getOutputChannelMembers()) {
			ResourcePath id = cm.getResource();
			StateTransition st = new StateTransition();
			String resName = id.getResourceName();
			Variable curVar = new Variable(resName + "1");
			st.setCurStateExpression(curVar);
			st.setNextStateExpression(rhs);
			Term message = new Term(update);
			for (Variable var: resToNextVar.values()) {
				message.addChild(var);
			}
			st.setMessageExpression(message);
			cm.setStateTransition(st);
		}
	}
	
	public Expression getFormulaTerm() {
		return formulaRhs;
	}
	
	public void setFormula(String formula) {
		this.formula = formula;
	}
	
	public String getFormula() {
		return formula;
	}
}
