package models.controlFlowModel;

import java.util.ArrayList;
import java.util.List;

import models.Edge;
import models.Node;

/*************************************************************
*
*/
public class ObjectNode extends Node {
	protected String name = "";
	
	/*************************************************************
	 * [ *constructor]
	/*************************************************************
	 */
	public ObjectNode(String name) {
		inEdges = new ArrayList<>();
		outEdges = new ArrayList<>();		
		
		this.name = name;
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * 
	 */	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public CallEdge getInEdge(int i) {
		return (CallEdge) ((List<Edge>) inEdges).get(i);
	}

	public CallEdge getOutEdge(int i) {
		return (CallEdge) ((List<Edge>) outEdges).get(i);
	}

	public CallEdge findEdgeInInEdges(final CallEdge edge) {
		for(Edge e : inEdges) {
			if( e instanceof CallEdge) return (CallEdge)e;
		}
		return null;
	}
	
	public void insertOutEdge(CallEdge edge, int n) {
		((List<Edge>) outEdges).add(n, edge);
	}
	
	public CallEdge findEdgeInOutEdges(final CallEdge edge) {
		for(Edge e : outEdges) {
			if( e instanceof CallEdge) return (CallEdge)e;
		}
		return null;
	}
	
	public int getChildrenNum() {
		return outEdges.size();
	}
	
	public int getOutEdgeCallOrder(final CallEdge callEdge) {
		for(int i = 0; i < outEdges.size(); i++) {
			if(callEdge.equals(getOutEdge(i))) return i;
		}
		return -1;
	}

	public ObjectNode getChildren(int i) {
		return (ObjectNode) ((List<Edge>) outEdges).get(i).getDestination();
	}
	
	/*************************************************************
	 * 
	 */
	public void sortOutEdgesByCallOrder(final int curOrder, final int callOrder) {
		ArrayList<Edge> edges = ((ArrayList<Edge>)outEdges);
		Edge edge = ((List<Edge>)outEdges).get(curOrder);

		edges.remove(curOrder);
		edges.add(callOrder-1, edge);
	}
}
