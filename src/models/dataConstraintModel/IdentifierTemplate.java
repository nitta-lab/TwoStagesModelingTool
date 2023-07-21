package models.dataConstraintModel;

import models.algebra.Expression;
import models.algebra.Term;
import models.algebra.Type;

public class IdentifierTemplate {
	private String resourceName = null;
	private Type resourceStateType = null;
	private int numParameters = 0;
	private Expression initialValue = null;
	protected String initText = null;
	
	public IdentifierTemplate(String resourceName, int numParameters) {
		this.resourceName = resourceName;
		this.numParameters =numParameters;
	}

	public IdentifierTemplate(String resourceName, Type resourceStateType, int numParameters) {
		this.resourceName = resourceName;
		this.resourceStateType = resourceStateType;
		this.numParameters = numParameters;
	}

	public String getResourceName() {
		return resourceName;
	}
	
	public int getNumberOfParameters() {
		return numParameters;
	}

	public Type getResourceStateType() {
		return resourceStateType;
	}

	public void setResourceStateType(Type resourceStateType) {
		this.resourceStateType = resourceStateType;
		if (initialValue != null) {
			if (initialValue instanceof Term) {
				((Term) initialValue).setType(resourceStateType);
			}
		}
	}
	
	public Expression getInitialValue() {
		return initialValue;
	}

	public void setInitialValue(Expression initialValue) {
		this.initialValue = initialValue;
	}

	public void setInitText(String initText) {
		this.initText = initText;
	}
	
	public String getInitText() {
		return initText;
	}

	public boolean equals(Object another) {
		if (!(another instanceof IdentifierTemplate)) return false;
		return resourceName.equals(((IdentifierTemplate) another).resourceName);
	}
	
	public int hashCode() {
		return resourceName.hashCode();
	}
}
