package algorithms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import models.*;
import models.algebra.*;
import models.dataConstraintModel.*;
import models.dataFlowModel.*;

/**
 * Algorithms to analyze data transfer model.
 * 
 * @author Nitta
 *
 */
public class DataTransferModelAnalyzer {
	/**
	 * Create data flow graph annotated with node attributes that indicate whether each resource state needs to be stored.
	 * @param model a data transfer model
	 * @return annotated data flow graph
	 */
	static public DataFlowGraph createDataFlowGraphWithStateStoringAttribute(DataTransferModel model) {
		DataFlowGraph graph = model.getDataFlowGraph();
		Collection<Channel> channels = new HashSet<>(model.getIOChannels());
		channels.addAll(model.getChannels());
		for (Channel channel: channels) {
			for (ChannelMember member: ((DataTransferChannel) channel).getOutputChannelMembers()) {
				boolean toBeStored = !member.getStateTransition().isRightUnary();		// The state does not need to be stored if the state transition function is right unary.
				for (Node node: graph.getNodes()) {
					if (((ResourceNode) node).getResource().equals(member.getResource())) {
						setStoreAttribute((ResourceNode) node, toBeStored);
					}
				}
			}
		}
		for (Node node: graph.getNodes()) {
			HashSet<Channel> inChannels = new HashSet<>();
			for(Edge inEdge: ((ResourceNode) node).getInEdges()) {
				inChannels.add(((DataFlowEdge) inEdge).getChannel());
			}
			if ((inChannels.size() > 1)) {
				// If the resource has multiple input channels, then the state of the resource needs to be stored.
				setStoreAttribute((ResourceNode) node, true);
			} else if (((ResourceNode) node).getAttribute() == null) {
				setStoreAttribute((ResourceNode) node, false);
			}
		}
		return graph;
	}

	static private void setStoreAttribute(ResourceNode node, boolean toBeStored) {
		NodeAttribute attr = node.getAttribute();
		StoreAttribute store;
		if (attr != null && attr instanceof NodeAttribute) {
			store = (StoreAttribute) attr;
			store.setNeeded(store.isNeeded() || toBeStored);
		} else {
			store = new StoreAttribute();
			store.setNeeded(toBeStored);
			node.setAttribute(store);
		}
	}

	/**
	 * Annotate data flow graph with edge attributes that indicate selectable data transfer methods.
	 * @param graph a data flow graph
	 * @return annotated data flow graph
	 */
	static public DataFlowGraph annotateWithSelectableDataTransferAttiribute(DataFlowGraph graph) {
		List<Node> nodes = new ArrayList<>(graph.getNodes());
		// set push only attributes
		for (Node n: graph.getNodes()) {
			if (nodes.contains(n) && ((StoreAttribute) ((ResourceNode) n).getAttribute()).isNeeded()) {
				nodes.remove(n);
				trackEdges(n, nodes);
			}
		}
		// set push/pull attributes to the remaining edges
		for (Edge e : graph.getEdges()) {
			if (((DataFlowEdge) e).getAttribute() == null) {
				PushPullAttribute ppat = new PushPullAttribute();
				ppat.addOption(PushPullValue.PUSHorPULL);
				ppat.addOption(PushPullValue.PUSH);
				ppat.addOption(PushPullValue.PULL);
				((DataFlowEdge) e).setAttribute(ppat);
			}
		}
		return graph;
	}

	static private void trackEdges(Node n, List<Node> nodes) {
		// recursively set push only attributes to input side edges
		for (Edge e : ((ResourceNode) n).getInEdges()) {
			PushPullAttribute ppat = new PushPullAttribute();
			ppat.addOption(PushPullValue.PUSH);
			((DataFlowEdge) e).setAttribute(ppat);
			Node n2 = e.getSource();
			if (nodes.contains(n2)) {
				nodes.remove(n2);
				trackEdges(n2, nodes);
			}
		}
	}
}
