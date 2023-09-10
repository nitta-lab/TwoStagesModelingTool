package models.dataFlowModel;

import java.util.Map;
import java.util.Set;

import models.Node;

public interface IFlowGraph {
	abstract public Map<Node, Set<Node>> getAllNodes();
}
