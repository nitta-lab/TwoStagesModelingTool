package models.dataFlowModel;

import models.Node;
import models.dataConstraintModel.ResourcePath;

public class ResourceNode extends Node {
	protected ResourcePath resourcePath = null;

	public ResourceNode(ResourcePath resourcePath) {
		this.resourcePath = resourcePath;
	}
	
	public ResourcePath getResource() {
		return resourcePath;
	}
	
	public boolean equals(Object another) {
		if (this == another) return true;
		if (!(another instanceof ResourceNode)) return false;
		return resourcePath.equals(((ResourceNode)another).resourcePath);
	}
	
	public int hashCode() {
		return resourcePath.hashCode();
	}
	
	public String toString() {
		return resourcePath.getResourceName();
	}
}
