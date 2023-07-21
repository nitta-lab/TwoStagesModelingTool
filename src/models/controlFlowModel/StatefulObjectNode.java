package models.controlFlowModel;

import models.dataFlowModel.ResourceNode;

public class StatefulObjectNode extends ObjectNode {
	private ResourceNode resource;
	
	public StatefulObjectNode(ResourceNode resource) {
		super(resource.getIdentifierTemplate().getResourceName());
		this.resource = resource;
	}

	public ResourceNode getResource() {
		return resource;
	}
	
}
