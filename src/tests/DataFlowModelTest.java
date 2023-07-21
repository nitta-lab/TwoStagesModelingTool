package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import models.*;
import models.dataConstraintModel.*;
import models.dataFlowModel.*;

public class DataFlowModelTest {

	@Test
	public void test() {
		// Construct a data-flow architecture model.
		DataTransferModel model = new DataTransferModel();
		IdentifierTemplate customer_off = new IdentifierTemplate("customers.{x1}.off", 1);		// an identifier template to specify a customer's office resource
		IdentifierTemplate company_add = new IdentifierTemplate("companies.{x2}.add", 1);		// an identifier template to specify a companie's address resource
		IdentifierTemplate customer_add = new IdentifierTemplate("customers.{x1}.add", 1);		// an identifier template to specify a customer's address resource
		
		// === gin_1 ===
		//
		// customers.{x1}.off(c, set(x)) == x
		// customers.{x1}.off(c, e) == c
		//
		DataTransferChannelGenerator gin_1 = new DataTransferChannelGenerator("gin_1");	// set customer's office (an input channel)
		GroupSelector x1 = new GroupSelector();
		ChannelMember customer_off_1 = new ChannelMember(customer_off);
		customer_off_1.addSelector(x1);				// x1 is determined by the path parameter in customer's office template, and serves as a group selector in this channel
		gin_1.addChannelMember(customer_off_1);
		assertEquals(customer_off.getNumberOfParameters(), customer_off_1.getSelectors().size());
		
		// === gin_2 ===
		//
		// companies.{x2}.add(a, set(y)) == y
		// companies.{x2}.add(a, e) == a
		//
		DataTransferChannelGenerator gin_2 = new DataTransferChannelGenerator("gin_2");	// set companie's address (an input channel)
		GroupSelector x2 = new GroupSelector();
		ChannelMember company_add_1 = new ChannelMember(company_add);
		company_add_1.addSelector(x2);				// x2 is determined by the path parameter in companie's address template, and serves as a group selector in this channel
		gin_2.addChannelMember(company_add_1);		
		assertEquals(company_add.getNumberOfParameters(), company_add_1.getSelectors().size());
		
		// === g ===
		//		
		// customers.{x3}.off( c, update(y, z)) == y
		// companies.{y}.add( a1, update(y, z)) == z
		// customers.{x3}.add(a2, update(y, z)) == z
		//
		DataTransferChannelGenerator g = new DataTransferChannelGenerator("g");		// update customer's address
		GroupSelector x3 = new GroupSelector();
		ChannelSelector y = new ChannelSelector();
		ChannelMember customer_off_2 = new ChannelMember(customer_off);
		ChannelMember company_add_2 = new ChannelMember(company_add);
		ChannelMember customer_add_2 = new ChannelMember(customer_add);
		customer_off_2.addSelector(x3);				// x3 is determined by the path parameter in customer's office template, and serves as a group selector in this channel
		company_add_2.addSelector(y);				// y is determined by the value of the customer's office resource, and serves as a channel selector in this channel
		customer_add_2.addSelector(x3);				// x3 determines the path parameter in customer's address template to update
		g.addChannelMemberAsInput(customer_off_2);
		g.addChannelMemberAsInput(customer_add_2);
		g.addChannelMemberAsOutput(company_add_2);
		assertEquals(customer_off.getNumberOfParameters(), customer_off_2.getSelectors().size());
		assertEquals(customer_add.getNumberOfParameters(), customer_add_2.getSelectors().size());
		assertEquals(company_add.getNumberOfParameters(), company_add_2.getSelectors().size());
		
		// Construct a data-flow architecture model.
		model.addIOChannelGenerator(gin_1);
		model.addIOChannelGenerator(gin_2);
		model.addChannelGenerator(g);
		
		// Check the model.
		assertEquals(3, model.getIdentifierTemplates().size());
		assertEquals(2, model.getIOChannelGenerators().size());
		assertEquals(1, model.getChannelGenerators().size());
		
		// Extract the resource dependency graph.
		DataFlowGraph resourceDependencyGraph = model.getDataFlowGraph();
		
		// Check the graph.
		assertEquals(3, resourceDependencyGraph.getNodes().size());
		assertEquals(2, resourceDependencyGraph.getEdges().size());
		for (Edge e: resourceDependencyGraph.getEdges()) {
			System.out.println(e.getSource() + "-(" + e + ")->" + e.getDestination());
		}
	}

}
