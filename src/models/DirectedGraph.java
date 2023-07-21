package models;

import java.util.HashSet;
import java.util.Set;

public class DirectedGraph {
	private Set<Node> nodes = null;
	private Set<Edge> edges = null;
	
	public DirectedGraph() {
		nodes = new HashSet<>();
		edges = new HashSet<>();
	}
	
	public Set<Node> getNodes() {
		return nodes;
	}
	
	public void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}
	
	public void addNode(Node node) {
		nodes.add(node);
	}
	
	public void removeNode(Node node) {
		nodes.remove(node);
		node.clearInEdges();
		node.clearOutEdges();
		for (Edge edge: edges) {
			if (edge.getSource().equals(node)) {
				edges.remove(edge);
			} else if (edge.getDestination().equals(node)) {
				edges.remove(edge);
			}
		}
	}
	
	public Set<Edge> getEdges() {
		return edges;
	}
	
	public void setEdges(Set<Edge> edges) {
		this.edges = edges;
		for (Edge edge: edges) {
			if (!nodes.contains(edge.getSource())) nodes.add(edge.getSource());
			if (!nodes.contains(edge.getDestination())) nodes.add(edge.getDestination());
			edge.getSource().addOutEdge(edge);
			edge.getDestination().addInEdge(edge);
		}
	}
	
	public void addEdge(Edge edge) {
		edges.add(edge);
		if (!nodes.contains(edge.getSource())) nodes.add(edge.getSource());
		if (!nodes.contains(edge.getDestination())) nodes.add(edge.getDestination());
		edge.getSource().addOutEdge(edge);
		edge.getDestination().addInEdge(edge);
	}
	
	public void removeEdge(Edge edge) {
		edges.remove(edge);
		edge.getSource().removeOutEdge(edge);
		edge.getDestination().removeInEdge(edge);
	}
	
	protected void simpleAddEdge(Edge edge) {
		edges.add(edge);
	}
}
