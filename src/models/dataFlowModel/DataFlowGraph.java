package models.dataFlowModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.DirectedGraph;
import models.Node;
import models.dataConstraintModel.ResourcePath;

public class DataFlowGraph extends DirectedGraph implements IFlowGraph {
	protected Map<ResourcePath, ResourceNode> nodeMap = null;
	
	public DataFlowGraph() {
		super();
		nodeMap = new HashMap<>();
	}
	
	public void addNode(ResourcePath id) {
		if (nodeMap.get(id) == null) {
			ResourceNode node = new ResourceNode(id);
			addNode(node);
			nodeMap.put(id, node);			
		}
	}

	public void addEdge(ResourcePath in, ResourcePath out, DataTransferChannel dfChannel) {
		ResourceNode srcNode = nodeMap.get(in);
		if (srcNode == null) {
			srcNode = new ResourceNode(in);
			addNode(srcNode);
			nodeMap.put(in, srcNode);
		}
		ResourceNode dstNode = nodeMap.get(out);
		if (dstNode == null) {
			dstNode = new ResourceNode(out);
			addNode(dstNode);
			nodeMap.put(out, dstNode);
		}
		addEdge(new DataFlowEdge(srcNode, dstNode, dfChannel));
	}
	
	public Collection<ResourceNode> getResouceNodes(){
		return nodeMap.values();
	}
	
	public ResourceNode getResouceNode(ResourcePath resourcePath) {
//		if(nodeMap.get(identifierTemplate) == null) throw new NullPointerException(identifierTemplate.getResourceName() + " was not found.");	// Because with this statement, the original JumpGame.model cannot be read.
		return nodeMap.get(resourcePath);
	}

	@Override
	public Set<Node> getAllNodes() {
		return super.getNodes();
	}
}
