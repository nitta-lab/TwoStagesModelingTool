package parser;

import java.io.BufferedReader;
import java.io.IOException;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;

import application.editor.Stage;
import models.dataFlowModel.DataTransferModel;
import parser.exceptions.ExpectedAssignment;
import parser.exceptions.ExpectedChannel;
import parser.exceptions.ExpectedChannelName;
import parser.exceptions.ExpectedEquals;
import parser.exceptions.ExpectedFormulaChannel;
import parser.exceptions.ExpectedGeometry;
import parser.exceptions.ExpectedInOrOutOrRefKeyword;
import parser.exceptions.ExpectedIoChannel;
import parser.exceptions.ExpectedLeftCurlyBracket;
import parser.exceptions.ExpectedModel;
import parser.exceptions.ExpectedNode;
import parser.exceptions.ExpectedRHSExpression;
import parser.exceptions.ExpectedResource;
import parser.exceptions.ExpectedRightBracket;
import parser.exceptions.ExpectedStateTransition;
import parser.exceptions.WrongLHSExpression;
import parser.exceptions.WrongRHSExpression;

public class ParserDTRAM extends Parser {

	private static final String MODEL_GROUP = "model";
	private static final String GEOMETRY_GROUP = "geometry";
	private static final String GEOMETORY_NODE = "node";
	private static final String RESOURCE_NODE = "r";
	private static final String CHANNEL_NODE = "c";
	private static final String FORMULA_CHANNEL_NODE = "fc";
	private static final String IO_CHANNEL_NODE = "ioc";

	/**--------------------------------------------------------------------------------
	 * [Constructor]
	/**--------------------------------------------------------------------------------
	 * 
	 * @param stream
	 */
	public ParserDTRAM(final TokenStream stream) {
		super(stream);
	}
	/**--------------------------------------------------------------------------------
	 * 
	 * @param reader
	 */
	public ParserDTRAM(final BufferedReader reader) {
		super(reader);
	}

	/**--------------------------------------------------------------------------------
	 * [public]
	/**--------------------------------------------------------------------------------
	 * 
	 * @param reader
	 */
	public DataTransferModel doParseModel() 
			throws ExpectedRightBracket, ExpectedChannel, ExpectedChannelName, ExpectedLeftCurlyBracket, ExpectedInOrOutOrRefKeyword, ExpectedStateTransition, ExpectedEquals, ExpectedRHSExpression, WrongLHSExpression, WrongRHSExpression, ExpectedAssignment, ExpectedModel, ExpectedGeometry {
		DataTransferModel model = getParsedModel();
		return model;
	}

	/**--------------------------------------------------------------------------------
	 * 
	 * @param graph
	 */
	public void doParseGeometry(mxGraph graph) 
			throws ExpectedRightBracket, ExpectedChannel, ExpectedChannelName, ExpectedLeftCurlyBracket, ExpectedInOrOutOrRefKeyword, ExpectedStateTransition, ExpectedEquals, ExpectedRHSExpression, WrongLHSExpression, WrongRHSExpression, ExpectedAssignment, ExpectedModel, ExpectedGeometry, ExpectedNode, ExpectedResource, ExpectedFormulaChannel, ExpectedIoChannel{

		parseGeometry(graph);
	}

	/**--------------------------------------------------------------------------------
	 * [private]
	/**--------------------------------------------------------------------------------
	 * 
	 * @param stream
	 */
	private DataTransferModel getParsedModel()
			throws ExpectedRightBracket, ExpectedChannel, ExpectedChannelName, ExpectedLeftCurlyBracket, ExpectedInOrOutOrRefKeyword, ExpectedStateTransition, ExpectedEquals, ExpectedRHSExpression, WrongLHSExpression, WrongRHSExpression, ExpectedAssignment, ExpectedModel, ExpectedGeometry  {

		if (!stream.hasNext()) throw new NullPointerException();

		String modelKeyword = stream.next();
		if (!modelKeyword.equals(MODEL_GROUP)) throw new ExpectedModel(stream.getLine());
		if (!stream.hasNext()) throw new ExpectedModel(stream.getLine());

		String leftBracket = stream.next();
		if (!leftBracket.equals(LEFT_CURLY_BRACKET)) throw new ExpectedLeftCurlyBracket(stream.getLine());

		DataTransferModel model = parseDataFlowModel();

		String rightBracket = stream.next();
		if(!rightBracket.equals(RIGHT_CURLY_BRACKET))throw new ExpectedRightBracket(stream.getLine());

		return model;
	}

	/**--------------------------------------------------------------------------------
	 * change graph's geometries from "DTRAM" file. 
	 * @param stream
	 * @param graph
	 */
	private void parseGeometry(mxGraph graph)
			throws ExpectedRightBracket, ExpectedChannel, ExpectedChannelName, ExpectedLeftCurlyBracket, ExpectedInOrOutOrRefKeyword, ExpectedStateTransition, ExpectedEquals, ExpectedRHSExpression, WrongLHSExpression, WrongRHSExpression, ExpectedAssignment,ExpectedModel, ExpectedGeometry, ExpectedNode, ExpectedResource, ExpectedFormulaChannel, ExpectedIoChannel {

		if (!isMatchKeyword(stream.next(), GEOMETRY_GROUP)) throw new ExpectedGeometry(stream.getLine());

		if (!isMatchKeyword(stream.next(), LEFT_CURLY_BRACKET)) throw new ExpectedLeftCurlyBracket(stream.getLine());

		String node = stream.next();
		while (node.equals(GEOMETORY_NODE)) {

			String rOrFcOrIocOrC = stream.next();
			if (!rOrFcOrIocOrC.equals(RESOURCE_NODE)
					&& !rOrFcOrIocOrC.equals(FORMULA_CHANNEL_NODE)
					&& !rOrFcOrIocOrC.equals(CHANNEL_NODE)
					&& !rOrFcOrIocOrC.equals(IO_CHANNEL_NODE))
				throw new ExpectedNode(stream.getLine());

			String name = stream.next();

			if (!isMatchKeyword(stream.next(), COLON)) throw new ExpectedAssignment(stream.getLine());

			String x = stream.next();
			int xC = Integer.parseInt(x);  // C = Coordinate(x,y,w,h)

			if (!isMatchKeyword(stream.next(), COMMA))throw new ExpectedAssignment(stream.getLine());

			String y = stream.next();
			int yC = Integer.parseInt(y);

			if (!isMatchKeyword(stream.next(), COMMA))throw new ExpectedAssignment(stream.getLine());

			String w = stream.next();
			int wC = Integer.parseInt(w);

			if (!isMatchKeyword(stream.next(), COMMA))throw new ExpectedAssignment(stream.getLine());

			String h = stream.next();
			int hC = Integer.parseInt(h);

			mxIGraphModel graphModel = graph.getModel();
			mxCell root = (mxCell) graph.getDefaultParent();
			mxCell nodeLayer = (mxCell) root.getChildAt(Stage.NODE_LAYER);
			mxCell dataFlowLayer = (mxCell) root.getChildAt(Stage.DATA_FLOW_LAYER);
			for (int i = 0; i < graph.getModel().getChildCount(nodeLayer); i++) {

				Object cell = graph.getModel().getChildAt(nodeLayer, i);
				if (!graph.getModel().isVertex(cell)) continue;

				mxGeometry geom = (mxGeometry) ((mxCell) cell).getGeometry().clone();
				mxGraphView view = graph.getView();
				mxCellState state = view.getState(cell);

				if (!name.equals(state.getLabel())) continue;

				geom.setX(xC);
				geom.setY(yC);
				graphModel.setGeometry(cell, geom);
			}
			for (int i = 0; i < graph.getModel().getChildCount(dataFlowLayer); i++) {

				Object cell = graph.getModel().getChildAt(dataFlowLayer, i);
				if (!graph.getModel().isVertex(cell)) continue;

				mxGeometry geom = (mxGeometry) ((mxCell) cell).getGeometry().clone();
				mxGraphView view = graph.getView();
				mxCellState state = view.getState(cell);

				if (!name.equals(state.getLabel())) continue;

				geom.setX(xC);
				geom.setY(yC);
				graphModel.setGeometry(cell, geom);
			}
			node = stream.next();
		}

		if (!node.equals(RIGHT_CURLY_BRACKET)) throw new ExpectedRightBracket(stream.getLine());
	}
}


