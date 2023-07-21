package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Node implements Cloneable, Serializable {
	protected Collection<Edge> inEdges = null;
	protected Collection<Edge> outEdges = null;
	private NodeAttribute attribute;
	
	public Node() {
		inEdges = new HashSet<>();
		outEdges = new HashSet<>();
	}
	
	public Collection<Edge> getInEdges() {
		return inEdges;
	}
	
	public void setInEdges(Collection<Edge> inEdges) {
		this.inEdges = inEdges;
	}
	
	public Collection<Edge> getOutEdges() {
		return outEdges;
	}
	
	public void setOutEdges(Collection<Edge> outEdges) {
		this.outEdges = outEdges;
	}
	
	public void addInEdge(Edge edge) {
		inEdges.add(edge);
	}
	
	public void addOutEdge(Edge edge) {
		outEdges.add(edge);
	}
	
	public void removeInEdge(Edge edge) {
		inEdges.remove(edge);
	}
	
	public void removeOutEdge(Edge edge) {
		outEdges.remove(edge);
	}

	public void clearInEdges() {
		inEdges.clear();
	}

	public void clearOutEdges() {
		outEdges.clear();
	}
	
	public int getIndegree() {
		return inEdges.size();
	}
	
	public int getOutdegree() {
		return outEdges.size();
	}
	
	public Collection<Node> getPredecessors() {
		Collection<Node> predecessors = new ArrayList<Node>();
		for (Edge edge: inEdges) {
			predecessors.add(edge.getSource());
		}
		return predecessors;
	}
	
	public Collection<Node> getSuccessors() {
		Collection<Node> successors = new ArrayList<Node>();
		for (Edge edge: outEdges) {
			successors.add(edge.getDestination());
		}
		return successors;
	}

	public NodeAttribute getAttribute() {
		return attribute;
	}

	public void setAttribute(NodeAttribute attribute) {
		this.attribute = attribute;
	}
}
