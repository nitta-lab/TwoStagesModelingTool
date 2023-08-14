package models.controlFlowModel;

import java.util.HashSet;
import java.util.Set;

import models.Edge;
import models.Node;
import models.algebra.Expression;
import models.algebra.Term;
import models.algebra.Variable;
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ChannelMember;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataFlowGraph;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.IFlowGraph;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResourceNode;

public class ControlFlowGraph implements IFlowGraph {
	private DataFlowGraph dataFlowGraph;
	private CallGraph pushCallGraph;
	private CallGraph pullCallGraph;
	
	public ControlFlowGraph(DataFlowGraph dataFlowGraph, DataTransferModel model) {
		this.dataFlowGraph = dataFlowGraph;
		this.pushCallGraph = new CallGraph();
		this.pullCallGraph = new CallGraph();
		for (Edge e: dataFlowGraph.getEdges()) {
			PushPullAttribute pushPull = ((PushPullAttribute) ((DataFlowEdge) e).getAttribute());
			ResourceNode srcNode = (ResourceNode) e.getSource();
			ResourceNode dstNode = (ResourceNode) e.getDestination();			
			if (pushPull.getOptions().get(0) == PushPullValue.PUSH) {
				// same direction as the data flow
				pushCallGraph.addEdge(srcNode, dstNode, PushPullValue.PUSH);
			} else {
				// reverse direction to the data flow
				pullCallGraph.addEdge(dstNode, srcNode, PushPullValue.PULL);
			}
		}
		for (Channel ch: model.getIOChannels()) {
			DataTransferChannel cio = (DataTransferChannel) ch;
			EntryPointObjectNode srcNode = new EntryPointObjectNode(cio);
			for (ChannelMember cm: cio.getChannelMembers()) {
				if (srcNode.getName() == null) {
					Expression exp = cm.getStateTransition().getMessageExpression();
					if (exp instanceof Term) {
						srcNode.setName(((Term) exp).getSymbol().getName());
					} else if (exp instanceof Variable) {
						srcNode.setName(((Variable) exp).getName());
					}
				}
				ResourceNode dstResNode = dataFlowGraph.getResouceNode(cm.getResource());
				StatefulObjectNode dstNode = pushCallGraph.getStatefulObjectNode(dstResNode);
				if (dstNode == null) {
					pushCallGraph.addNode(dstResNode);
					dstNode = pushCallGraph.getStatefulObjectNode(dstResNode);
				}
				// from an I/O channel to a resource
				pushCallGraph.insertEdge(srcNode, dstNode, PushPullValue.PUSH, 0);
			}
		}
	}
	
	public ControlFlowGraph(DataFlowGraph dataFlowGraph, PushPullValue priority) {
		this.dataFlowGraph = dataFlowGraph;
		this.pushCallGraph = new CallGraph();
		this.pullCallGraph = new CallGraph();
		if (priority == PushPullValue.PUSH) {
			// push-first
			for (Edge e: dataFlowGraph.getEdges()) {
				ResourceNode srcNode = (ResourceNode) e.getSource();
				ResourceNode dstNode = (ResourceNode) e.getDestination();			
				// same direction as the data flow
				pushCallGraph.addEdge(srcNode, dstNode, PushPullValue.PUSH);
			}			
		} else {
			// pull-first
			for (Edge e: dataFlowGraph.getEdges()) {
				ResourceNode srcNode = (ResourceNode) e.getSource();
				ResourceNode dstNode = (ResourceNode) e.getDestination();
				PushPullAttribute pushPull = ((PushPullAttribute) ((DataFlowEdge) e).getAttribute());
				if (pushPull.getOptions().contains(PushPullValue.PULL)) {
					// Pull style is selectable
					// reverse direction to the data flow
					pullCallGraph.addEdge(dstNode, srcNode, PushPullValue.PULL);
				} else {
					// Pull style is not selectable
					// same direction as the data flow
					pushCallGraph.addEdge(srcNode, dstNode, PushPullValue.PUSH);
				}
			}
		}
	}

	public DataFlowGraph getDataFlowGraph() {
		return dataFlowGraph;
	}

	public CallGraph getPushCallGraph() {
		return pushCallGraph;
	}

	public CallGraph getPullCallGraph() {
		return pullCallGraph;
	}
	
	@Override
	public Set<Node> getAllNodes() {
		Set<Node> allNodes = new HashSet<>();
		allNodes.addAll(pushCallGraph.getNodes());
		for (Node n: pullCallGraph.getNodes()) {
			if (n instanceof StatefulObjectNode) {
				if (pushCallGraph.getStatefulObjectNode(((StatefulObjectNode) n).getResource()) == null) {
					allNodes.add(n);
				}
			} else {
				allNodes.add(n);
			}
		}
		return allNodes;
	}
}
