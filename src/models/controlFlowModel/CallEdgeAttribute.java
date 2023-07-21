package models.controlFlowModel;

import com.mxgraph.model.mxCell;

import models.EdgeAttribute;
import models.dataFlowModel.PushPullValue;

/*************************************************************
 * Information of connection status of the call edge among nodes type of "Object-Node".
 */
public class CallEdgeAttribute extends EdgeAttribute {
	private CallEdge callEdge = null;
	private ObjectNode orginalSrcObjNode = null;
	private mxCell srcCell = null;
	private mxCell dstCell = null;
	
	/*************************************************************
	 *  [ *constructor ]
	/*************************************************************
	 * 
	 */
	public CallEdgeAttribute(CallEdge callEdge, ObjectNode originalSrcObjNode, final mxCell srcCell, final mxCell dstCell) {
		this.callEdge = callEdge;
		this.callEdge.setAttribute(this);
		
		this.orginalSrcObjNode = originalSrcObjNode;
		
		this.srcCell = srcCell;
		this.dstCell = dstCell;
	}

	/*************************************************************
	 * 
	 */
	public CallEdgeAttribute(CallEdge callEdge, final mxCell srcCell, final mxCell dstCell) {
		this.callEdge = callEdge;
		this.callEdge.setAttribute(this);
		
		this.srcCell = srcCell;
		this.dstCell = dstCell;
	}

	
	/*************************************************************
	 *  [ *public ]
	/*************************************************************
	 *  [ getter ]
	/*************************************************************/
	public CallEdge getCallEdge() {
		return callEdge;
	}
	
	public ObjectNode getOriginalSourceObjectNode() {
		return orginalSrcObjNode;
	}
	
	public PushPullValue getSelectedOption() {
		return callEdge.getSelectedOption();
	}
		
	public ObjectNode getSourceObjectNode() {
		if( !(callEdge.getSource() instanceof ObjectNode) ) throw new ClassCastException("sourceNode isn't type of <ObjectNode>"); 
		return (ObjectNode)callEdge.getSource();
	}
	
	public ObjectNode getDestinationObjectNode() {
		if( !(callEdge.getDestination() instanceof ObjectNode) ) throw new ClassCastException("destinationNode isn't type of <ObjectNode>"); 
		return (ObjectNode)callEdge.getDestination();
	}

	public mxCell getSourceCell() {
		return srcCell;
	}
	
	public mxCell getDestinationCell() {
		return dstCell;
	}
	
	public void setDestinationCell(final mxCell dstCell) {
		this.dstCell = dstCell;
	}
	
	/*************************************************************
	 * 
	 */
	@Override
	public String toString() {
		String value = "";
		
		if(2 <= callEdge.getSource().getOutEdges( ).size()) {
			int order = (((ObjectNode)callEdge.getSource()).getOutEdgeCallOrder(callEdge)+ 1);
			value += "[" + order + "]";
		}
		
		return value;
	}
	
}
