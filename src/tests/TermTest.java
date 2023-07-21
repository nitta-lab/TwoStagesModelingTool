package tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;

import models.algebra.Constant;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Variable;

public class TermTest {

	@Test
	public void test() {
		Symbol add = new Symbol("add", 2);
		Symbol mul = new Symbol("mul", 2);
		Constant one = new Constant("1");
		Constant two = new Constant("2");
		Variable x = new Variable("x");
		Variable y = new Variable("y");
		Variable z = new Variable("z");
		Term t1 = new Term(add);		// add(1, x)
		t1.addChild(one);
		t1.addChild(x);
		Term t2 = new Term(mul);		// mul(add(1, x), y)
		t2.addChild(t1);
		t2.addChild(y);
		Term t3 = new Term(add);		// add(1, x)
		t3.addChild(one);
		t3.addChild(x);
		
		assertTrue(t1.contains(x));
		assertFalse(t1.contains(y));
		assertTrue(t2.contains(x));
		assertTrue(t2.contains(y));
		assertTrue(t2.contains(t1));
		assertTrue(t2.contains(t3));
		assertFalse(t2.contains(z));
		assertFalse(t2.contains(two));
		assertTrue(one.equals(one));
		assertTrue(two.equals(two));
		assertTrue(x.equals(x));
		assertTrue(y.equals(y));
		assertFalse(x.equals(y));
		assertFalse(x.equals(one));
		assertFalse(t1.equals(x));
		System.out.println(t1);
		System.out.println(t2);
	}

}
