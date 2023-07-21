package models;

import java.io.Serializable;

public class Edge implements Serializable {
	protected Node source;
	protected Node destination;
	private EdgeAttribute attribute;
	
	public Edge(Node src, Node dst) {
		source = src;
		destination = dst;
	}
	
	public Node getSource() {
		return source;
	}
	
	public void setSource(Node source) {
		this.source = source;
	}
	
	public Node getDestination() {
		return destination;
	}
	
	public void setDestination(Node destination) {
		this.destination = destination;
	}

	public EdgeAttribute getAttribute() {
		return attribute;
	}

	public void setAttribute(EdgeAttribute attribute) {
		this.attribute = attribute;
	}
}
