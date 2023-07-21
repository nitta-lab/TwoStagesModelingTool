package application.editor;

import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResourceNode;

public class InEdgeAttribute extends PushPullAttribute {
	private ResourceNode srcRes;
	
	public InEdgeAttribute(PushPullAttribute attr, ResourceNode srcRes) {
		super();
		super.setOptions(attr.getOptions());
		this.srcRes = srcRes;
	}

	public ResourceNode getSrcResource() {
		return srcRes;
	}
}
