package generators;

import java.util.HashSet;

import models.*;
import models.algebra.*;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataFlowModel.*;

/**
 * Algorithm to analyze data transfer methods selected by a user
 * 
 * @author Nitta
 *
 */
public class DataTransferMethodAnalyzer {
	static private HashSet<Node> reachableNodes = new HashSet<>();

	/**
	 * Determine whether each resource state is stored or not depending on selected data transfer methods.
	 * 
	 * @param graph a data flow graph (in/out)
	 */
	static public void decideToStoreResourceStates(DataFlowGraph graph) {
		reachableNodes.clear();
		for (Node n : graph.getNodes()) {
			ResourceNode resource = (ResourceNode) n;
			trackNode(resource);
		}
	}

	static private void trackNode(ResourceNode resource) {
		if (reachableNodes.contains(resource))
			return;
		reachableNodes.add(resource);
		boolean flag = true;
		for (Edge e : resource.getInEdges()) {
			if (((PushPullAttribute) e.getAttribute()).getOptions().get(0) != PushPullValue.PUSH) {
				// Traverse pull edges only.
				trackNode((ResourceNode) e.getSource());
				flag = false;
			}
		}
		((StoreAttribute) resource.getAttribute()).setStored(flag);
	}
}
