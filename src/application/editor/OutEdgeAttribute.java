package application.editor;

import application.editor.Editor.SrcDstAttribute;
import models.EdgeAttribute;
import models.dataFlowModel.ResourceNode;

public class OutEdgeAttribute extends SrcDstAttribute {
	private ResourceNode dstRes;
	
	public OutEdgeAttribute(SrcDstAttribute attr, ResourceNode dstRes) {
		super(attr.getSrouce(), attr.getDestination());
		this.dstRes = dstRes;
	}
	
	public ResourceNode getDstResource() {
		return dstRes;
	}
}
