package tests.controlFlowModel;

import org.junit.Test;

import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataTransferModel;

import static org.junit.Assert.*;


/**--------------------------------------------------------------------------------
 * 
 */
public class ControlFlowDelegatorTest {
	@Test
	public void test() {
		// Construct a data-flow architecture model.
		DataTransferModel model = new DataTransferModel();
		ResourcePath customer_off = new ResourcePath("customers.{x1}.off", 1);		// a resource to specify a customer's office resource
		ResourcePath company_add = new ResourcePath("companies.{x2}.add", 1);		// a resource to specify a companie's address resource
		ResourcePath customer_add = new ResourcePath("customers.{x1}.add", 1);		// a resource to specify a customer's address resource
		
		
	
	}
}
