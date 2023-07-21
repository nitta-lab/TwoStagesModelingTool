package tests;

import org.junit.Test;

import models.algebra.Symbol;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.IdentifierTemplate;
import models.visualModel.FormulaChannelGenerator;

public class FormulaChannelTest {

	@Test
	public void test() {
		IdentifierTemplate id1 = new IdentifierTemplate("r1", 0);
		IdentifierTemplate id2 = new IdentifierTemplate("r2", 0);
		IdentifierTemplate id3 = new IdentifierTemplate("r3", 0);
		
		FormulaChannelGenerator ch1 = new FormulaChannelGenerator("ch1", DataConstraintModel.add);
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
		
		FormulaChannelGenerator ch2 = new FormulaChannelGenerator("ch2", DataConstraintModel.mul);
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
