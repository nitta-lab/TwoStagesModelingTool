package models.controlFlowModel;

import java.util.ArrayList;
import java.util.List;

import models.Edge;

/*************************************************************
 * it has Delegation of Control-Flow algorithm.
 */
public class ControlFlowDelegator {
	
	private ControlFlowGraph controlFlowGraph = null;
	
	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 * 
	 */
	public ControlFlowDelegator(final ControlFlowGraph controlFlowGraph) {
		this.controlFlowGraph = controlFlowGraph;
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	* 
	*@param callEdge 
	*/
	public List<ObjectNode> searchDelegatableNodes(final CallEdge callEdge){
		List<ObjectNode> nodes = new ArrayList<>();
		
		// 1. adding parentNode
		ObjectNode delegatingNode = (ObjectNode) callEdge.getDestination();
		ObjectNode parentNode = (ObjectNode) callEdge.getSource();

		if(parentNode == null || delegatingNode == null) 
			throw new NullPointerException("parentNode is null.");
		if( !(parentNode  instanceof ObjectNode && delegatingNode instanceof ObjectNode)) 
			throw new ClassCastException("callEdge.getSource() is not ObjectNode");
		
		// if the relation of "delegatingNode" to "parentNode" is 1 : 1 ?
		// then return an empty list.
		if( isRootNode(parentNode) && ! hasChildrenNode(parentNode) ) return nodes;

		// 2. collecting for each transfer method has nodes in the common area.
		collectCommonTransferNodes(nodes, parentNode, delegatingNode);
		
		// 3. if the transfer method is PUSH-style,
		// 		then serach delegatable area.
		collectParentNodesInPushTransfer(nodes, delegatingNode, parentNode);
		
		// switch objects by transfer type
		return nodes;
	}
	
	/*************************************************************
	 *
	 */
	public void delegateCallEdge(CallEdge delegatingEdge, final ObjectNode dstObjNode) {		
		ObjectNode srcObjNode = (ObjectNode)delegatingEdge.getDestination();
		if(srcObjNode == null) throw new ClassCastException();
	
		delegatingEdge.getSource().removeOutEdge(delegatingEdge);
		srcObjNode.removeInEdge(delegatingEdge);
	
		// Reconnecting the edge to the new source object.
		delegatingEdge.setDestination(srcObjNode);
		delegatingEdge.setSource(dstObjNode);	

		srcObjNode.addInEdge(delegatingEdge);
		dstObjNode.addOutEdge(delegatingEdge);
	}	
	
	/*************************************************************
	 *  [* private ]
	/*************************************************************
	 *  [   search  ]
	/*************************************************************
	 * Collecting nodes in the "nodes" parameter for each transfer method has nodes in the common area.
	 * @param nodes
	 * @param curObjNode
	 * @param delegatingObjNode
	 * @param selectedOption
	 */
	private void collectCommonTransferNodes(List<ObjectNode> nodes, ObjectNode curObjNode, final ObjectNode delegatingObjNode){				
		if( !hasChildrenNode(curObjNode)) return;

		for(Edge e : curObjNode.getOutEdges()) {
			ObjectNode foundNode = (ObjectNode)e.getDestination();					
			
			if( foundNode.equals(delegatingObjNode))  continue;
			
			nodes.add(foundNode);
			collectCommonTransferNodes(nodes, foundNode, delegatingObjNode);
		}
	}

	/*************************************************************
	 * Collecting nodes in the "nodes" parameter for node of the area of PUSH-style transfer method.
	 * @param result in "nodes" parameter.
	 * @param curObjNode
	 */
	private void collectParentNodesInPushTransfer(List<ObjectNode> nodes, ObjectNode curObjNode, final ObjectNode parentDelegatingNode) {
		if( isRootNode(curObjNode) ) return;
		if( isInEdgesConversingToNode(curObjNode) ) return;	
			
		ObjectNode parentObjNode = (ObjectNode)curObjNode.getInEdge(0).getSource();
		if(parentObjNode == null) return;		
		
		if( !parentDelegatingNode.equals(parentObjNode) )
			nodes.add(parentObjNode);

		int inEdgeCallOrder = parentObjNode.getOutEdgeCallOrder(curObjNode.getInEdge(0));
		for(Edge edge : parentObjNode.getOutEdges()) {
			if( !(edge instanceof CallEdge)) continue;

			int callOrder = parentObjNode.getOutEdgeCallOrder((CallEdge)edge);
			if(inEdgeCallOrder < callOrder) collectChildNodesInPushTransfer(nodes, (CallEdge)edge);
		}	
				
		collectParentNodesInPushTransfer(nodes, parentObjNode, parentDelegatingNode);
	}

	/*************************************************************
	 * 
	 * @param node
	 */
	private void collectChildNodesInPushTransfer(List<ObjectNode> nodes, CallEdge callEdge) {
		ObjectNode dstObjNode = (ObjectNode)callEdge.getDestination();
		if(dstObjNode == null) return;

		nodes.add(dstObjNode);
		
		if(!hasChildrenNode(dstObjNode)) return;
		
		for(Edge e : dstObjNode.getOutEdges()) {
			CallEdge edge = (CallEdge)e;
			if(edge == null) continue;
			
			ObjectNode foundNode = (ObjectNode)e.getDestination();	
			if(foundNode == null) continue;
			if(nodes.contains(foundNode))continue;
			
			collectChildNodesInPushTransfer(nodes, edge);
		}
	}
		
	/*************************************************************
	 * 
	 * @param node
	 */
	private boolean isRootNode(final ObjectNode node) {
		return node.getInEdges().isEmpty();
	}
	
	/*************************************************************
	 * 
	 * @param node
	 */	
	private boolean hasChildrenNode(final ObjectNode node) {
		if(node.getOutEdges().size() < 1) return false;
		return true;
	}

	/*************************************************************
	 * 
	 * @param node
	 */	
	private boolean isInEdgesConversingToNode(final ObjectNode node) {
		return ( 1 < node.getInEdges().size() );
	}
}
