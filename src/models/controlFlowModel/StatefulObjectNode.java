package models.controlFlowModel;

import models.dataFlowModel.ResourceNode;

public class StatefulObjectNode extends ObjectNode {
	private ResourceNode resource;
	
	public StatefulObjectNode(ResourceNode resource) {
		super(resource.getResource().getResourceName());
		this.resource = resource;
	}

	public ResourceNode getResource() {
		return resource;
	}
	
}
