package tests;

import static org.junit.Assert.*;

import org.junit.Test;

import models.dataConstraintModel.*;

public class DataConstraintModelTest {

	@Test
	public void test() {
		// Construct a data constraint architecture model.
		DataConstraintModel model = new DataConstraintModel();
		IdentifierTemplate customer_off = new IdentifierTemplate("customers.{customer_id}.off", 1);
		IdentifierTemplate customer_add = new IdentifierTemplate("customers.{customer_id}.add", 1);
		IdentifierTemplate company_add = new IdentifierTemplate("companies.{company_id}.add", 1);
		
		ChannelGenerator gin_1 = new ChannelGenerator("gin_1");	// set customer's office
		GroupSelector x1 = new GroupSelector();
		ChannelMember customer_off_1 = new ChannelMember(customer_off);
		customer_off_1.addSelector(x1);
		gin_1.addChannelMember(customer_off_1);
		assertEquals(customer_off.getNumberOfParameters(), customer_off_1.getSelectors().size());
		
		ChannelGenerator gin_2 = new ChannelGenerator("gin_2");	// set companie's address
		GroupSelector x2 = new GroupSelector();
		ChannelMember company_add_1 = new ChannelMember(company_add);
		company_add_1.addSelector(x2);
		gin_2.addChannelMember(company_add_1);		
		assertEquals(company_add.getNumberOfParameters(), company_add_1.getSelectors().size());
		
		ChannelGenerator g = new ChannelGenerator("g");		// update customer's address
		GroupSelector x3 = new GroupSelector();
		ChannelSelector y = new ChannelSelector();
		ChannelMember customer_off_2 = new ChannelMember(customer_off);
		ChannelMember company_add_2 = new ChannelMember(company_add);
		ChannelMember customer_add_2 = new ChannelMember(customer_add);
		customer_off_2.addSelector(x3);
		company_add_2.addSelector(y);
		customer_add_2.addSelector(x3);
		g.addChannelMember(customer_off_2);
		g.addChannelMember(customer_add_2);
		g.addChannelMember(company_add_2);
		assertEquals(customer_off.getNumberOfParameters(), customer_off_2.getSelectors().size());
		assertEquals(customer_add.getNumberOfParameters(), customer_add_2.getSelectors().size());
		assertEquals(company_add.getNumberOfParameters(), company_add_2.getSelectors().size());
		
		model.addIOChannelGenerator(gin_1);
		model.addIOChannelGenerator(gin_2);
		model.addChannelGenerator(g);
		
		// Check the model.
		assertEquals(3, model.getIdentifierTemplates().size());
		assertEquals(2, model.getIOChannelGenerators().size());
		assertEquals(1, model.getChannelGenerators().size());
	}

}
