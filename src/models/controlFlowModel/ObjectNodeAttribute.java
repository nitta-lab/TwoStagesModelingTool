package models.controlFlowModel;

import models.NodeAttribute;

/*************************************************************
 * 
 * @author k-fujii
 * 
 */
public class ObjectNodeAttribute extends NodeAttribute {
	private ObjectNode objectNode = null;
	private String shapeStyle = "";

	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 * 
	 */
	public ObjectNodeAttribute(final ObjectNode objectNode) {
		this.objectNode = objectNode;
		this.objectNode.setAttribute(this);

		// Setting a shape style of cell
		if(objectNode instanceof StatefulObjectNode) {
			shapeStyle = "shape=ellipse;perimeter=ellipsePerimeter;";
		}
		else if(objectNode instanceof EntryPointObjectNode) {
			shapeStyle = "shape=rectangle;perimeter=rectanglePerimeter;";
		}
		else {
			shapeStyle = "shape=hexagon;perimeter=hexagonPerimeter;";
		}

		// Setting a name of cell
		if(objectNode.name != null) return;
		if(objectNode.name.isEmpty()) return;
		
		if( objectNode instanceof StatefulObjectNode ) {
			objectNode.name = objectNode.getName();			
		}
		else if(objectNode instanceof EntryPointObjectNode){
			objectNode.name = "entryPoint";
		}
	}

	/*************************************************************
	*  [ *public ]
	/*************************************************************
	 * [ getter ]
 	/*************************************************************
	 * 
	 * @return
	 */
	public ObjectNode getObjectNode() {
		return objectNode;
	}
	
	/*************************************************************
	 * 
	 */
	public String getDefaultStyle() {
		String style = ";";
		
		return objectNode instanceof StatefulObjectNode
					? shapeStyle + style
					: shapeStyle + style;
	}

	/*************************************************************
	 * 
	 */
	public String getEnableStyle() {
		String style = "fillColor=#7fffd4;";
		style += "strokeColor=#66cdaa;";
		style += "strokeWidth=2;";

		return objectNode instanceof StatefulObjectNode
					? shapeStyle + style
					: shapeStyle + style;
	}
	
	/*************************************************************
	 * 
	 */
	public String getDisableStyle() {
		String style = "fillColor=#999999";
		
		return objectNode instanceof StatefulObjectNode
					? shapeStyle + style
					: shapeStyle + style;
	}
		
	/*************************************************************
	 * 
	 */
	public String getDelegatingStyle() {
		String style = "strokeWidth=4;";
		style += "strokeColor=#4169e;";

		return shapeStyle + style;
	}

	/*************************************************************
	 * showing label of mxCell
	 */
	@Override
	public String toString() {
		return objectNode instanceof StatefulObjectNode
					? ((StatefulObjectNode) objectNode).getResource().getIdentifierTemplate().getResourceName()
					:  objectNode.getName();
	}
}
