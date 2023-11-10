package tests.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import models.Edge;
import models.algebra.InvalidMessage;
import models.algebra.ParameterizedIdentifierIsFutureWork;
import models.algebra.UnificationFailed;
import models.algebra.ValueUndefined;
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ChannelMember;
import models.dataFlowModel.*;
import parser.Parser;
import parser.exceptions.ExpectedAssignment;
import parser.exceptions.ExpectedChannel;
import parser.exceptions.ExpectedChannelName;
import parser.exceptions.ExpectedColon;
import parser.exceptions.ExpectedEquals;
import parser.exceptions.ExpectedInOrOutOrRefKeyword;
import parser.exceptions.ExpectedLeftCurlyBracket;
import parser.exceptions.ExpectedRHSExpression;
import parser.exceptions.ExpectedRightBracket;
import parser.exceptions.ExpectedStateTransition;
import parser.exceptions.WrongJsonExpression;
import parser.exceptions.WrongLHSExpression;
import parser.exceptions.WrongRHSExpression;

public class ParseTest {

	public static void main(String[] args) {
		File file = new File("models/POS.model");
		try {
			Parser parser = new Parser(new BufferedReader(new FileReader(file)));
			DataTransferModel model;
			try {
				model = parser.doParse();
				System.out.println(model);

				for (Channel c: model.getChannels()) {
					for (ChannelMember out: ((DataTransferChannel) c).getOutputChannelMembers()) {
						String[] sideEffects = new String[] {""};
						System.out.println("next" + out.getResource().getResourceName() + " = " + ((DataTransferChannel) c).deriveUpdateExpressionOf(out).toImplementation(sideEffects));
					}
				}

				System.out.println();

				DataFlowGraph resourceDependencyGraph = model.getDataFlowGraph();
				for (Edge e: resourceDependencyGraph.getEdges()) {
					System.out.println(e.getSource() + "-(" + e + ")->" + e.getDestination());
				}
			} catch (ExpectedChannel | ExpectedChannelName | ExpectedLeftCurlyBracket | ExpectedInOrOutOrRefKeyword
					| ExpectedStateTransition | ExpectedEquals | ExpectedRHSExpression | WrongLHSExpression
					| WrongRHSExpression | ExpectedRightBracket | ParameterizedIdentifierIsFutureWork 
					| ResolvingMultipleDefinitionIsFutureWork | InvalidMessage
					| UnificationFailed | ValueUndefined | ExpectedAssignment | WrongJsonExpression | ExpectedColon e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
