package tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;

import org.junit.Test;

import models.algebra.Expression;
import models.algebra.Position;
import models.algebra.Variable;
import models.dataFlowModel.DataTransferModel;
import parser.Parser;
import parser.Parser.TokenStream;
import parser.exceptions.ExpectedRightBracket;

public class InverseTest {
	@Test
	public void test() {
		String lhs = "y";
		DataTransferModel model = new DataTransferModel();
		try {
			String rhs = "(a * x + b) * c";
			
			TokenStream stream = new Parser.TokenStream();
			Parser parser = new Parser(stream);
			stream.addLine(rhs);				

			Expression rhsExp = parser.parseTerm(stream, model);
			System.out.println(lhs + " = " + rhsExp);

			HashMap<Position, Variable> rhsVars = rhsExp.getVariables();
			assertEquals(4, rhsVars.size());

			// Solve {y = (a * x + b) + c} for a, b, c, x
			Variable y = new Variable(lhs);
			for (Position vPos: rhsVars.keySet()) {
				Variable v = rhsVars.get(vPos);
				Expression inv = rhsExp.getInverseMap(y, vPos);		// inverse map to get v back from the output value y
				assertTrue(inv.contains(y));
				assertFalse(inv.contains(v));
				System.out.println(rhsVars.get(vPos) + " = " + inv);
			}
		} catch (ExpectedRightBracket e) {
			e.printStackTrace();
		}
	}
}
