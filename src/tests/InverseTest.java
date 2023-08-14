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
import models.algebra.Term;
import models.algebra.Variable;
import models.dataFlowModel.DataTransferModel;
import parser.Parser;
import parser.Parser.TokenStream;
import parser.exceptions.ExpectedRightBracket;

public class InverseTest {
	@Test
	public void test() {
		DataTransferModel model = new DataTransferModel();
		try {
			String lhs = "y";
			String rhs = "(a * x + b) * c";
			
			TokenStream stream = new Parser.TokenStream();
			Parser parser = new Parser(stream);
			stream.addLine(rhs);				

			Expression rhsExp = parser.parseTerm(stream, model);
			System.out.println("=== solve{" + lhs + " = " + rhsExp + "} for a, b, d, x ===");

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
			
			// Extract an element in a tuple
			TokenStream stream2 = new Parser.TokenStream();
			Parser parser2 = new Parser(stream2);
			stream2.addLine("fst(tuple(x, y))");
			Expression tupleExp = parser2.parseTerm(stream2, model);
			stream2.addLine("snd(tuple(x, y))");
			Expression tupleExp2 = parser2.parseTerm(stream2, model);
			Expression reduced = ((Term) tupleExp).reduce();
			Expression reduced2 = ((Term) tupleExp2).reduce();
			Variable x = new Variable("x");
			assertEquals(reduced, x);
			assertEquals(reduced2, y);
			System.out.println("=== simplify ===");
			System.out.println(tupleExp + " = " + reduced);
			System.out.println(tupleExp2 + " = " + reduced2);
			
			// Solve {z = fst(x)} for x
			TokenStream stream3 = new Parser.TokenStream();
			Parser parser3 = new Parser(stream3);
			stream3.addLine("fst(x)");
			Expression rhsExp3 = parser3.parseTerm(stream3, model);
			Variable z = new Variable("z");
			System.out.println("=== solve{" + z + " = " + rhsExp3 + "} for x ===");
			HashMap<Position, Variable> rhsVars3 = rhsExp3.getVariables();
			for (Position vPos: rhsVars3.keySet()) {
				Variable v = rhsVars3.get(vPos);
				Expression inv = rhsExp3.getInverseMap(z, vPos);		// inverse map to get v back from the output value y
				if (inv instanceof Term) {
					inv = ((Term) inv).reduce();
				}
				assertTrue(inv.contains(z));
				assertFalse(inv.contains(v));
				System.out.println(rhsVars3.get(vPos) + " = " + inv);
			}			
		} catch (ExpectedRightBracket e) {
			e.printStackTrace();
		}
	}
}
