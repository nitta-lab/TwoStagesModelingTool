package models.dataFlowModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.DirectedGraph;
import models.Node;
import models.dataConstraintModel.IdentifierTemplate;

public class DataFlowGraph extends DirectedGraph implements IFlowGraph {
	protected Map<IdentifierTemplate, ResourceNode> nodeMap = null;
	
	public DataFlowGraph() {
		super();
		nodeMap = new HashMap<>();
	}
	
	public void addNode(IdentifierTemplate id) {
		if (nodeMap.get(id) == null) {
			ResourceNode node = new ResourceNode(id);
			addNode(node);
			nodeMap.put(id, node);			
		}
	}

	public void addEdge(IdentifierTemplate in, IdentifierTemplate out, DataTransferChannelGenerator dfChannelGen) {
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
		addEdge(new DataFlowEdge(srcNode, dstNode, dfChannelGen));
	}
	
	public Collection<ResourceNode> getResouceNodes(){
		return nodeMap.values();
	}
	
	public ResourceNode getResouceNode(IdentifierTemplate identifierTemplate) {
//		if(nodeMap.get(identifierTemplate) == null) throw new NullPointerException(identifierTemplate.getResourceName() + " was not found.");	// Because with this statement, the original JumpGame.model cannot be read.
		return nodeMap.get(identifierTemplate);
	}

	@Override
	public Set<Node> getAllNodes() {
		return super.getNodes();
	}
}
