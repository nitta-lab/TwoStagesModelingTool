package models.dataFlowModel;

import models.NodeAttribute;

/*************************************************************
 * 
 * @author k-fujii
 *
 */
public class ResourceNodeAttribute extends NodeAttribute {
	private ResourceNode resourceNode = null;

	/*************************************************************
	 * [ *constructor ]
	 /*************************************************************
	 * 
	 */
	public ResourceNodeAttribute(ResourceNode resNode) {
		this.resourceNode = resNode;
		this.resourceNode.setAttribute(this);
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * [ getter ]
 	 /*************************************************************
	 * 
	 * @return
	 */
	public ResourceNode getResourceNode() {
		return resourceNode;
	}
	
	/*************************************************************
	 * 
	 */
	public String getResourceName() {
		return resourceNode.getResource().getResourceName();
	}

	/*************************************************************
	 * 
	 * @return
	 */	
	public String getDefaultStyle() {
		String style ="";
		style += "shape=ellipse;";
		style += "perimeter=ellipsePerimeter";

		return style;
	}

	
	/*************************************************************
	 * 
	 * @return
	 */
	@Override
	public String toString() {
		return resourceNode.getResource().getResourceName();
	}
}
