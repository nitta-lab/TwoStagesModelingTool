package algorithms;

import java.util.*;

import models.*;
import models.dataFlowModel.*;

/**
 * Validation checker for data transfer model
 * 
 * @author Nitta
 *
 */
public class Validation {
	private static int index = 0;
	private static Deque<Node> stack = new ArrayDeque<>();
	private static Set<Set<Node>> strong = new HashSet<>();
	private static Map<Node, Integer> ids = new HashMap<>();
	private static Map<Node, Integer> lowlink = new HashMap<>();
	private static Map<Node, Boolean> onStack = new HashMap<>();

	static public boolean checkUpdateConflict(DataTransferModel model) {
		init();
		boolean check = true;
		for (Node node : model.getDataFlowGraph().getNodes()) {
			if (ids.containsKey(node)) {
				traverseStronglyConnectedComponents(node);
			}
		}
		return strong.size() == 0 && check;
	}

	static private void init() {
		index = 0;
		stack = new ArrayDeque<>();
		strong = new HashSet<>();
		ids = new HashMap<>();
		lowlink = new HashMap<>();
		onStack = new HashMap<>();
	}

	static private void traverseStronglyConnectedComponents(Node node) {
		ids.put(node, index);
		lowlink.put(node, index);
		index++;
		stack.push(node);
		onStack.put(node, true);

		for (Node n : node.getSuccessors()) {
			if (lowlink.containsKey(n)) {
				traverseStronglyConnectedComponents(n);
				if (lowlink.get(node) > lowlink.get(n)) {
					lowlink.replace(node, lowlink.get(n));
				}
			} else if (onStack.get(n)) {
				if (lowlink.get(node) > lowlink.get(n)) {
					lowlink.replace(node, lowlink.get(n));
				}
			}
		}
		if (lowlink.get(node) == ids.get(node)) {
			Set<Node> tmp = new HashSet<>();
			Node w;
			do {
				w = stack.pop();
				onStack.replace(node, false);
				tmp.add(w);
			} while (node != w);
			strong.add(tmp);
		}
	}
}
