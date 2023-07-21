package tests;

import models.algebra.Constant;
import models.algebra.Expression;
import models.algebra.Field;
import models.algebra.InvalidMessage;
import models.algebra.Parameter;
import models.algebra.ParameterizedIdentifierIsFutureWork;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.UnificationFailed;
import models.algebra.ValueUndefined;
import models.algebra.Variable;
import models.dataConstraintModel.*;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.DataTransferChannelGenerator;
import models.dataFlowModel.DataTransferChannelGenerator.IResourceStateAccessor;
import models.dataFlowModel.ResolvingMultipleDefinitionIsFutureWork;

public class UpdateCodeGenerationTest {
	
	public static void main(String[] args) {
		// Pre-defined symbols
		Symbol floor = new Symbol("floor", 1, Symbol.Type.PREFIX, "(int)Math.floor", Symbol.Type.PREFIX);
		Symbol sum = new Symbol("sum", 1, Symbol.Type.PREFIX, "stream().mapToInt(x->x).sum", Symbol.Type.METHOD);
		
		// resources
		IdentifierTemplate payment = new IdentifierTemplate("payment", DataConstraintModel.typeInt, 0);	// an identifier template to specify the payment resource
		IdentifierTemplate loyalty = new IdentifierTemplate("loyalty", DataConstraintModel.typeInt, 0);	// an identifier template to specify the loyalty resource
		IdentifierTemplate history = new IdentifierTemplate("history", DataConstraintModel.typeList, 0);// an identifier template to specify the payment history resource
		IdentifierTemplate total = new IdentifierTemplate("total", DataConstraintModel.typeInt, 0);		// an identifier template to specify the total payment resource
		
		// fields in the Java program
		final Field fPayment = new Field("payment", DataConstraintModel.typeInt);
		final Field fLoyalty = new Field("loyalty", DataConstraintModel.typeInt);
		final Field fHistory = new Field("history", DataConstraintModel.typeList);
		final Field fTotal = new Field("total", DataConstraintModel.typeInt);
		// parameters in the Java program
		final Parameter pPayment = new Parameter("payment", DataConstraintModel.typeInt);
		final Parameter pLoyalty = new Parameter("loyalty", DataConstraintModel.typeInt);
		final Parameter pHistory = new Parameter("history", DataConstraintModel.typeList);
		final Parameter pTotal = new Parameter("total", DataConstraintModel.typeInt);
		IResourceStateAccessor pushAccessor = new IResourceStateAccessor() {
			@Override
			public Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				if (target.equals(from)) {
					if (target.equals(payment)) return fPayment;
					if (target.equals(loyalty)) return fLoyalty;
					if (target.equals(history)) return fHistory;
					if (target.equals(total)) return fTotal;
				}
				return null;
			}
			@Override
			public Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				if (target.equals(payment)) return pPayment;
				if (target.equals(loyalty)) return pLoyalty;
				if (target.equals(history)) return pHistory;
				if (target.equals(total)) return pTotal;
				return null;
			}				
		};
		
		// methods in the Java program
		final Symbol paymentGetter = new Symbol("getPayment", 1, Symbol.Type.METHOD);
		final Symbol loyltyGetter = new Symbol("getLoyalty", 1, Symbol.Type.METHOD);
		final Symbol historyGetter = new Symbol("getHistory", 1, Symbol.Type.METHOD);
		final Symbol totalGetter = new Symbol("getTotal", 1, Symbol.Type.METHOD);
		IResourceStateAccessor pullAccessor = new IResourceStateAccessor() {
			@Override
			public Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				if (target.equals(from)) {
					if (target.equals(payment)) return fPayment;
					if (target.equals(loyalty)) return fLoyalty;
					if (target.equals(history)) return fHistory;
					if (target.equals(total)) return fTotal;
				}
				return null;
			}
			@Override
			public Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				if (target.equals(payment)) {
					Term getter = new Term(paymentGetter);
					getter.addChild(fPayment);
					return getter;
				}
				if (target.equals(loyalty)) {
					Term getter = new Term(loyltyGetter);
					getter.addChild(fLoyalty);
					return getter;
				}
				if (target.equals(history)) {
					Term getter = new Term(historyGetter);
					getter.addChild(fHistory);
					return getter;
				}
				if (target.equals(total)) {
					Term getter = new Term(totalGetter);
					getter.addChild(fTotal);
					return getter;
				}
				return null;
			}				
		};
		
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
		
		Variable p1 = new Variable("p1");
		Variable y = new Variable("y");
		Variable l = new Variable("l");
		Constant c_0_05 = new Constant("0.05");
		Symbol update1 = new Symbol("update1", 1);
		Term c1_message = new Term(update1);		// update1(y)
		c1_message.addChild(y);
		Term rawLoyality = new Term(DataConstraintModel.mul);			// y*0.05
		rawLoyality.addChild(y);
		rawLoyality.addChild(c_0_05);
		Term nextLoyality = new Term(floor);		// floor(y*0.05)
		nextLoyality.addChild(rawLoyality);
		
		StateTransition c1_payment_transition = new StateTransition();
		c1_payment_transition.setCurStateExpression(p1);
		c1_payment_transition.setMessageExpression(c1_message);
		c1_payment_transition.setNextStateExpression(y);
		c1_payment.setStateTransition(c1_payment_transition);
		
		StateTransition c1_loyalty_transition = new StateTransition();
		c1_loyalty_transition.setCurStateExpression(l);
		c1_loyalty_transition.setMessageExpression(c1_message);
		c1_loyalty_transition.setNextStateExpression(nextLoyality);
		c1_loyalty.setStateTransition(c1_loyalty_transition);
		
		System.out.println(c1);
		
		try {
			String[] sideEffects = new String[] {""};
			System.out.println("-----");			
			System.out.println(c1.deriveUpdateExpressionOf(c1_loyalty).toImplementation(sideEffects));
			
			System.out.println("-- PUSH --");
			Expression loyaltyPushUpdate = c1.deriveUpdateExpressionOf(c1_loyalty, pushAccessor);
			Parameter param = null;
			for (Parameter p: loyaltyPushUpdate.getSubTerms(Parameter.class).values()) {
				if (p.equals(pPayment) || p.equals(pLoyalty) || p.equals(pHistory) || p.equals(pTotal)) {
					param = p;
					break;
				}
			}
			System.out.println("void update(" + param.getType().getImplementationTypeName() + " " + param.toImplementation(sideEffects) + ") {");
			System.out.println("\t" + fLoyalty + " = " + loyaltyPushUpdate.toImplementation(sideEffects) + ";");
			System.out.println("}");
			
			System.out.println("-- PULL --");
			System.out.println(loyalty.getResourceStateType().getImplementationTypeName() + " " + loyltyGetter.toImplementation() + "() {");
			System.out.println("\t return " + c1.deriveUpdateExpressionOf(c1_loyalty, pullAccessor).toImplementation(sideEffects) + ";");
			System.out.println("}");
		} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork | InvalidMessage
				| UnificationFailed | ValueUndefined e) {
			e.printStackTrace();
		}
		
		System.out.println("==========");
		
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
		
		Variable z = new Variable("z");
		Variable h = new Variable("h");
		Symbol update2 = new Symbol("update2", 1);
		Term c2_message = new Term(update2);		// update2(z)
		c2_message.addChild(z);
		Term nextHistory = new Term(DataTransferModel.cons);	// cons(z, h)
		nextHistory.addChild(z);
		nextHistory.addChild(h);
		
		StateTransition c2_payment_transition = new StateTransition();
		c2_payment_transition.setCurStateExpression(p1);
		c2_payment_transition.setMessageExpression(c2_message);
		c2_payment_transition.setNextStateExpression(z);
		c2_payment.setStateTransition(c2_payment_transition);
		
		StateTransition c2_history_transition = new StateTransition();
		c2_history_transition.setCurStateExpression(h);
		c2_history_transition.setMessageExpression(c2_message);
		c2_history_transition.setNextStateExpression(nextHistory);
		c2_history.setStateTransition(c2_history_transition);
		
		System.out.println(c2);		
		
		try {
			String[] sideEffects = new String[] {""};
			System.out.println("-----");
			System.out.println(c2.deriveUpdateExpressionOf(c2_history).toImplementation(sideEffects));
			
			System.out.println("-- PUSH --");
			Expression historyPushUpdate = c2.deriveUpdateExpressionOf(c2_history, pushAccessor);
			Parameter param = null;
			for (Parameter p: historyPushUpdate.getSubTerms(Parameter.class).values()) {
				if (p.equals(pPayment) || p.equals(pLoyalty) || p.equals(pHistory) || p.equals(pTotal)) {
					param = p;
					break;
				}
			}
			System.out.println("void update(" + param.getType().getImplementationTypeName() + " " + param.toImplementation(sideEffects) + ") {");
			System.out.println("\t" + fHistory + " = " + historyPushUpdate.toImplementation(sideEffects) + ";");
			System.out.println("}");
			
			System.out.println("-- PULL --");
			System.out.println(history.getResourceStateType().getImplementationTypeName() + " " + historyGetter.toImplementation() + "() {");
			System.out.println("\t return " + c2.deriveUpdateExpressionOf(c2_history, pullAccessor).toImplementation(sideEffects) + ";");
			System.out.println("}");
		} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork | InvalidMessage
				| UnificationFailed | ValueUndefined e) {
			e.printStackTrace();
		}
		
		System.out.println("==========");
		
		// === c3 ===
		//
		// history(h, update3(u)) = u
		// total(t, update3(u)) = sum(u)
		//
		DataTransferChannelGenerator c3 = new DataTransferChannelGenerator("c3");
		ChannelMember c3_history = new ChannelMember(history);
		ChannelMember c3_total = new ChannelMember(total);
		c3.addChannelMemberAsInput(c3_history);
		c3.addChannelMemberAsOutput(c3_total);
		
		Variable u = new Variable("u");
		Variable t = new Variable("t");
		Symbol update3 = new Symbol("update3", 1);
		Term c3_message = new Term(update3);		// update3(u)
		c3_message.addChild(u);
		Expression nextHistory2 = u;
		Term nextTotal = new Term(sum);
		nextTotal.addChild(u);
		
		StateTransition c3_history_transition = new StateTransition();
		c3_history_transition.setCurStateExpression(h);
		c3_history_transition.setMessageExpression(c3_message);
		c3_history_transition.setNextStateExpression(nextHistory2);
		c3_history.setStateTransition(c3_history_transition);

		StateTransition c3_total_transition = new StateTransition();
		c3_total_transition.setCurStateExpression(t);
		c3_total_transition.setMessageExpression(c3_message);
		c3_total_transition.setNextStateExpression(nextTotal);
		c3_total.setStateTransition(c3_total_transition);
		
		System.out.println(c3);

		try {
			String[] sideEffects = new String[] {""};
			System.out.println("-----");
			System.out.println(c3.deriveUpdateExpressionOf(c3_total).toImplementation(sideEffects));
			
			System.out.println("-- PUSH --");
			Expression totalPushUpdate = c3.deriveUpdateExpressionOf(c3_total, pushAccessor);
			Parameter param = null;
			for (Parameter p: totalPushUpdate.getSubTerms(Parameter.class).values()) {
				if (p.equals(pPayment) || p.equals(pLoyalty) || p.equals(pHistory) || p.equals(pTotal)) {
					param = p;
					break;
				}
			}
			System.out.println("void update(" + param.getType().getImplementationTypeName() + " " + param.toImplementation(sideEffects) + ") {");
			System.out.println("\t" + fTotal + " = " + totalPushUpdate.toImplementation(sideEffects) + ";");
			System.out.println("}");
			
			System.out.println("-- PULL --");
			System.out.println(total.getResourceStateType().getImplementationTypeName() + " " + totalGetter.toImplementation() + "() {");
			System.out.println("\t return " + c3.deriveUpdateExpressionOf(c3_total, pullAccessor).toImplementation(sideEffects) + ";");
			System.out.println("}");
		} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork | InvalidMessage
				| UnificationFailed | ValueUndefined e) {
			e.printStackTrace();
		}
		
		System.out.println("==========");
	}

}
