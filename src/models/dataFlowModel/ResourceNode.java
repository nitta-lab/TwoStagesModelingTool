package models.dataFlowModel;

import models.Node;
import models.dataConstraintModel.IdentifierTemplate;

public class ResourceNode extends Node {
	protected IdentifierTemplate identifierTemplate = null;

	public ResourceNode(IdentifierTemplate identifierTemplate) {
		this.identifierTemplate = identifierTemplate;
	}
	
	public IdentifierTemplate getIdentifierTemplate() {
		return identifierTemplate;
	}
	
	public boolean equals(Object another) {
		if (this == another) return true;
		if (!(another instanceof ResourceNode)) return false;
		return identifierTemplate.equals(((ResourceNode)another).identifierTemplate);
	}
	
	public int hashCode() {
		return identifierTemplate.hashCode();
	}
	
	public String toString() {
		return identifierTemplate.getResourceName();
	}
}
