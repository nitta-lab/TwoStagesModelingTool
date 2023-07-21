package tests.controlFlowModel;

import org.junit.Test;

import models.dataConstraintModel.IdentifierTemplate;
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
		IdentifierTemplate customer_off = new IdentifierTemplate("customers.{x1}.off", 1);		// an identifier template to specify a customer's office resource
		IdentifierTemplate company_add = new IdentifierTemplate("companies.{x2}.add", 1);		// an identifier template to specify a companie's address resource
		IdentifierTemplate customer_add = new IdentifierTemplate("customers.{x1}.add", 1);		// an identifier template to specify a customer's address resource
		
		
	
	}
}
