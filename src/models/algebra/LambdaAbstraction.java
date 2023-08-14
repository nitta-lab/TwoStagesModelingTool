package models.algebra;

import java.util.ArrayList;
import java.util.List;

public class LambdaAbstraction extends Symbol {
	private List<Variable> variables = null;
	private Term term = null;
	
	public LambdaAbstraction(Variable variable, Term term) {
		super("($" + variable.getName() + ")->" + term.toString(), 1, Type.LAMBDA);
		this.variables = new ArrayList<>();
		this.variables.add(variable);
		this.term = term;
	}
	
	public LambdaAbstraction(List<Variable> variables, Term term) {
		super("($" + variables + ")->" + term.toString(), variables.size(), Type.LAMBDA);
		this.variables = variables;
		this.term = term;
	}

	public List<Variable> getVariables() {
		return variables;
	}

	public Term getTerm() {
		return term;
	}
}
