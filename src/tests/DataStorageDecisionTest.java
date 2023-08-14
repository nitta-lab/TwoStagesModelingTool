package tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import algorithms.*;
import generators.DataTransferMethodAnalyzer;
import models.Node;
import models.dataFlowModel.*;
import parser.*;
import parser.exceptions.ExpectedAssignment;
import parser.exceptions.ExpectedChannel;
import parser.exceptions.ExpectedChannelName;
import parser.exceptions.ExpectedEquals;
import parser.exceptions.ExpectedInOrOutOrRefKeyword;
import parser.exceptions.ExpectedLeftCurlyBracket;
import parser.exceptions.ExpectedRHSExpression;
import parser.exceptions.ExpectedRightBracket;
import parser.exceptions.ExpectedStateTransition;
import parser.exceptions.WrongLHSExpression;
import parser.exceptions.WrongRHSExpression;

public class DataStorageDecisionTest {
	public static void main(String[] args) {
		File file = new File("models/POS2.model");
		try {
			Parser parser = new Parser(new BufferedReader(new FileReader(file)));
			DataTransferModel model = null;
			try {
				model = parser.doParse();
				System.out.println(model);
				DataFlowGraph graph = DataTransferModelAnalyzer.createDataFlowGraphWithStateStoringAttribute(model);
				DataTransferModelAnalyzer.annotateWithSelectableDataTransferAttiribute(graph);
				DataTransferMethodAnalyzer.decideToStoreResourceStates(graph);
				for(Node n:graph.getNodes()) {
					System.out.println(((ResourceNode) n).getResource().getResourceName() + ":" + ((StoreAttribute) ((ResourceNode) n).getAttribute()).isStored());
				}
			} catch (ExpectedChannel | ExpectedChannelName | ExpectedLeftCurlyBracket | ExpectedInOrOutOrRefKeyword
					| ExpectedStateTransition | ExpectedEquals | ExpectedRHSExpression | WrongLHSExpression
					| WrongRHSExpression | ExpectedRightBracket | ExpectedAssignment e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
