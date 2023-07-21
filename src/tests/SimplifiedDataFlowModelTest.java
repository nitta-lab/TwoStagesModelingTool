package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import models.*;
import models.dataConstraintModel.*;
import models.dataFlowModel.*;

public class SimplifiedDataFlowModelTest {

	@Test
	public void test() {
		// Construct a data-flow architecture model.
		DataTransferModel model = new DataTransferModel();
		IdentifierTemplate payment = new IdentifierTemplate("payment", 0);	// an identifier template to specify the payment resource
		IdentifierTemplate loyalty = new IdentifierTemplate("loyalty", 0);	// an identifier template to specify the loyalty resource
		IdentifierTemplate history = new IdentifierTemplate("history", 0);	// an identifier template to specify the payment history resource
		IdentifierTemplate total = new IdentifierTemplate("total", 0);		// an identifier template to specify the total payment resource
		
		// === cin ===
		//
		// payment(p1, purchase(x)) == x
		//
		DataTransferChannelGenerator cin = new DataTransferChannelGenerator("cin");
		ChannelMember cin_payment = new ChannelMember(payment);
		cin.addChannelMember(cin_payment);
		assertEquals(cin.getChannelMembers().size(), 1);
		
		// === c1 ===
		//
		// payment(p1, update1(y)) == y
		// loyalty(l, update1(y)) == floor(y * 0.05)
		//
		DataTransferChannelGenerator c1 = new DataTransferChannelGenerator("c1");
		ChannelMember c1_payment = new ChannelMember(payment);
		ChannelMember c1_loyalty = new ChannelMember(loyalty);
		c1.addChannelMemberAsInput(c1_payment);
		c1.addChannelMemberAsOutput(c1_loyalty);
		assertEquals(c1.getChannelMembers().size(), 2);
		assertEquals(c1.getInputChannelMembers().size(), 1);
		assertEquals(c1.getOutputChannelMembers().size(), 1);
		
		// === c2 ===
		//
		// payment(p1, update2(z)) == z
		// history(h, update2(z)) == cons(z, h)
		//
		DataTransferChannelGenerator c2 = new DataTransferChannelGenerator("c2");
		ChannelMember c2_payment = new ChannelMember(payment);
		ChannelMember c2_history = new ChannelMember(history);
		c2.addChannelMemberAsInput(c2_payment);
		c2.addChannelMemberAsOutput(c2_history);
		assertEquals(c2.getChannelMembers().size(), 2);
		assertEquals(c2.getInputChannelMembers().size(), 1);
		assertEquals(c2.getOutputChannelMembers().size(), 1);
		
		// === c3 ===
		//
		// history(h, update3(u)) == u
		// total(t, update3(u)) == sum(u)
		//
		DataTransferChannelGenerator c3 = new DataTransferChannelGenerator("c3");
		ChannelMember c3_history = new ChannelMember(history);
		ChannelMember c3_total = new ChannelMember(total);
		c3.addChannelMemberAsInput(c3_history);
		c3.addChannelMemberAsOutput(c3_total);
		assertEquals(c3.getChannelMembers().size(), 2);
		assertEquals(c3.getInputChannelMembers().size(), 1);
		assertEquals(c3.getOutputChannelMembers().size(), 1);
		
		// Construct a data-flow architecture model.
		model.addIOChannelGenerator(cin);
		model.addChannelGenerator(c1);
		model.addChannelGenerator(c2);
		model.addChannelGenerator(c3);
		
		// Check the model.
		assertEquals(4, model.getIdentifierTemplates().size());
		assertEquals(1, model.getIOChannelGenerators().size());
		assertEquals(3, model.getChannelGenerators().size());
		
		// Extract the resource dependency graph.
		DataFlowGraph resourceDependencyGraph = model.getDataFlowGraph();
		
		// Check the graph.
		assertEquals(4, resourceDependencyGraph.getNodes().size());
		assertEquals(3, resourceDependencyGraph.getEdges().size());
		for (Edge e: resourceDependencyGraph.getEdges()) {
			System.out.println(e.getSource() + "-(" + e + ")->" + e.getDestination());
		}
	}

}
