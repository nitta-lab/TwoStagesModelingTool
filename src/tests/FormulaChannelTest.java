package tests;

import org.junit.Test;

import models.algebra.Symbol;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.ResourcePath;
import models.visualModel.FormulaChannel;

public class FormulaChannelTest {

	@Test
	public void test() {
		ResourcePath id1 = new ResourcePath("r1", 0);
		ResourcePath id2 = new ResourcePath("r2", 0);
		ResourcePath id3 = new ResourcePath("r3", 0);
		
		FormulaChannel ch1 = new FormulaChannel("ch1", DataConstraintModel.add);
		System.out.println(ch1.getFormula());
		System.out.println(ch1.getFormulaTerm());
		System.out.println(ch1.getSourceText());
		ch1.addChannelMemberAsInput(new ChannelMember(id1));
		System.out.println(ch1.getFormula());
		System.out.println(ch1.getFormulaTerm());
		System.out.println(ch1.getSourceText());
		ch1.addChannelMemberAsInput(new ChannelMember(id2));
		System.out.println(ch1.getFormula());
		System.out.println(ch1.getFormulaTerm());
		System.out.println(ch1.getSourceText());
		ch1.addChannelMemberAsOutput(new ChannelMember(id3));
		System.out.println(ch1.getFormula());
		System.out.println(ch1.getFormulaTerm());
		System.out.println(ch1.getSourceText());
		
		FormulaChannel ch2 = new FormulaChannel("ch2", DataConstraintModel.mul);
		System.out.println(ch2.getFormula());
		System.out.println(ch2.getFormulaTerm());
		System.out.println(ch2.getSourceText());
		ch2.addChannelMemberAsOutput(new ChannelMember(id3));
		System.out.println(ch2.getFormula());
		System.out.println(ch2.getFormulaTerm());
		System.out.println(ch2.getSourceText());
		ch2.addChannelMemberAsInput(new ChannelMember(id1));
		System.out.println(ch2.getFormula());
		System.out.println(ch2.getFormulaTerm());
		System.out.println(ch2.getSourceText());
		ch2.addChannelMemberAsInput(new ChannelMember(id2));
		System.out.println(ch2.getFormula());
		System.out.println(ch2.getFormulaTerm());
		System.out.println(ch2.getSourceText());
		
	}

}
