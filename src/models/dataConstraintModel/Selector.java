package models.dataConstraintModel;

import models.algebra.Variable;

public class Selector {
	private Variable variable = null;
	
	public Selector(Variable variable) {
		this.setVariable(variable);
	}

	public Variable getVariable() {
		return variable;
	}

	public void setVariable(Variable variable) {
		this.variable = variable;
	}
}
