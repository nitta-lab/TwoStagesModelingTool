package tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import algorithms.*;
import models.dataFlowModel.DataTransferModel;
import parser.Parser;
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

public class UpdateConflictCheckTest {
	public static void main(String[] args) {
		File file = new File("models/POS2.model");
		try {
			Parser parser = new Parser(new BufferedReader(new FileReader(file)));
			try {
				DataTransferModel model = parser.doParse();
				System.out.println(Validation.checkUpdateConflict(model));
			} catch (ExpectedRightBracket e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedChannel e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedChannelName e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedLeftCurlyBracket e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedInOrOutOrRefKeyword e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedStateTransition e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedEquals e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedRHSExpression e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongLHSExpression e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (WrongRHSExpression e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExpectedAssignment e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
