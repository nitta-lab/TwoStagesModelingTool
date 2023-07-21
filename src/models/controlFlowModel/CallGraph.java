package models.controlFlowModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import models.DirectedGraph;
import models.Node;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResourceNode;

public class CallGraph extends DirectedGraph {
	protected Map<ResourceNode, StatefulObjectNode> statefulObjMap = null;
	
	public CallGraph() {
		statefulObjMap = new HashMap<>();
	}
	
	public void addNode(Node node) {
		if (node instanceof ResourceNode) {
			ResourceNode resNode = (ResourceNode) node;
			StatefulObjectNode objNode = statefulObjMap.get(resNode);
			if (objNode == null) {
				objNode = new StatefulObjectNode(resNode);
				statefulObjMap.put(resNode, objNode);
				super.addNode(objNode);
			}
		} else if (node instanceof StatefulObjectNode) {
			StatefulObjectNode objNode = (StatefulObjectNode) node;
			if (statefulObjMap.get(objNode.getResource()) == null) {
				statefulObjMap.put(objNode.getResource(), objNode);
				super.addNode(objNode);
			}
		} else {
			super.addNode(node);
		}
	}

	public void addEdge(ResourceNode srcResNode, ResourceNode dstResNode, PushPullValue selectedOption) {
		addNode(srcResNode);
		addNode(dstResNode);
		addEdge(new CallEdge(getStatefulObjectNode(srcResNode), getStatefulObjectNode(dstResNode), selectedOption));
	}
	
	public void insertEdge(ObjectNode srcObjNode, ObjectNode dstObjNode, PushPullValue selectedOption, int n) {
		CallEdge edge = new CallEdge(srcObjNode, dstObjNode, selectedOption);
		simpleAddEdge(edge);
		addNode(srcObjNode);
		addNode(dstObjNode);
		srcObjNode.insertOutEdge(edge, n);
		dstObjNode.addInEdge(edge);
	}
	
	public StatefulObjectNode getStatefulObjectNode(ResourceNode resNode) {
		return statefulObjMap.get(resNode);
	}
	
	public Set<Node> getRootNodes() {
		Set<Node> roots = new HashSet<>(getNodes());
		for (Node n: getNodes()) {
			roots.removeAll(n.getSuccessors());
		}
		return roots;
	}
}
