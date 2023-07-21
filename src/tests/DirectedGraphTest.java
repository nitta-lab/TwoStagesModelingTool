package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import models.*;

public class DirectedGraphTest {

	@Test
	public void test() {
		// Build a directed graph.
		DirectedGraph g = new DirectedGraph();
		Node n1 = new Node();
		Node n2 = new Node();
		Node n3 = new Node();
		Node n4 = new Node();
		Edge e1 = new Edge(n1, n2);
		Edge e2 = new Edge(n2, n3);
		Edge e3 = new Edge(n3, n4);
		Edge e4 = new Edge(n4, n1);
		Edge e5 = new Edge(n1, n3);
		g.addEdge(e1);
		g.addEdge(e2);
		g.addEdge(e3);
		g.addEdge(e4);
		g.addEdge(e5);
		// Check the number of nodes.
		assertEquals(4, g.getNodes().size());
		// Check the number of edges.
		assertEquals(5, g.getEdges().size());
		// Check indegrees and outdegrees.
		assertEquals(1, n1.getIndegree());
		assertEquals(2, n1.getOutdegree());
		assertEquals(1, n2.getIndegree());
		assertEquals(1, n2.getOutdegree());
		assertEquals(2, n3.getIndegree());
		assertEquals(1, n3.getOutdegree());
		assertEquals(1, n4.getIndegree());
		assertEquals(1, n4.getOutdegree());
		
		// Remove an edge.
		g.removeEdge(e5);		
		// Re-check the number of nodes.
		assertEquals(4, g.getNodes().size());
		// Re-check the number of edges.
		assertEquals(4, g.getEdges().size());
		// Re-check indegrees and outdegrees.
		assertEquals(1, n1.getIndegree());
		assertEquals(1, n1.getOutdegree());
		assertEquals(1, n2.getIndegree());
		assertEquals(1, n2.getOutdegree());
		assertEquals(1, n3.getIndegree());
		assertEquals(1, n3.getOutdegree());
		assertEquals(1, n4.getIndegree());
		assertEquals(1, n4.getOutdegree());
	}

}
