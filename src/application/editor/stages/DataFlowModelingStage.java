package application.editor.stages;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import algorithms.Validation;
import application.editor.Editor;
import application.editor.Editor.SrcDstAttribute;
import application.editor.FlowCellEditor;
import application.editor.Stage;
import models.dataConstraintModel.ChannelGenerator;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.IdentifierTemplate;
import models.dataFlowModel.DataTransferChannelGenerator;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.ResourceNodeAttribute;
import models.visualModel.FormulaChannelGenerator;
import parser.Parser;
import parser.Parser.TokenStream;
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

/*************************************************************
 * 
 * @author n-nitta, k-fujii
 */
public class DataFlowModelingStage extends Stage {
	public int PORT_DIAMETER = 8;
	public int PORT_RADIUS = PORT_DIAMETER / 2;
	
	/*************************************************************
	 * [ *constructor]
	 /*************************************************************
	 * 
	 */
	public DataFlowModelingStage(mxGraphComponent graphComponent) {
		super(graphComponent);
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * 
	 */
	@Override
	public boolean canChangeFrom(Stage prevStage) {
		return true;
	}

	/*************************************************************
	 * 
	 */
	@Override
	public void init(Stage prevStage) {
		showOnlyLayer(DATA_FLOW_LAYER);
	}
	
	/*************************************************************
	 * 
	 */
	@Override
	public FlowCellEditor createCellEditor(mxGraphComponent graphComponent) {
		return new DataFlowCellEditor(this, graphComponent);
	}

	@Override
	public mxIEventListener createChangeEventListener(Editor editor) {
		return new mxIEventListener() {
			public void invoke(Object sender, mxEventObject evt) {
				List<mxCell> terminals = new ArrayList<>();
				mxCell cell = null;
				for (Object change: ((List) evt.getProperties().get("changes"))) {
					if (change instanceof mxGraphModel.mxTerminalChange) {
						mxGraphModel.mxTerminalChange terminalChange = (mxGraphModel.mxTerminalChange) change;
						cell = (mxCell) terminalChange.getCell();
						mxCell terminal = (mxCell) terminalChange.getTerminal();
						terminals.add(terminal);
					}
				}
				if (terminals.size() == 2) {
					if (!editor.connectEdge(cell, terminals.get(0), terminals.get(1))) {
						graph.removeCells(new mxCell[] {cell});
					}
				}
			}
		};
	}
	
	@Override
	public MouseListener createMouseEventListener(Editor editor) {
		return null;
	}
	
	/*************************************************************
	 * 
	 */
	public void clear() {
		model = null;
		((mxGraphModel) graph.getModel()).clear();
		
		// Construct layers.
		mxCell root = (mxCell) graph.getDefaultParent();
		graph.getModel().beginUpdate();
		try {
			root.insert(new mxCell());	// NODE_LAYER, DATA_FLOW_LAYER
			root.insert(new mxCell());	// PUSH_FLOW_LAYER
			root.insert(new mxCell());	// PULL_FLOW_LAYER
					
			showOnlyLayer(NODE_LAYER, DATA_FLOW_LAYER);
		} 
		finally {
			graph.getModel().endUpdate();
		}
		graph.refresh();
	}

	/*************************************************************
	 * 
	 */
	public DataTransferModel getModel() {
		if (model == null) {
			setModel(new DataTransferModel());
		}
		return model;
	}

	/*************************************************************
	 * 
	 */
	public void setModel(DataTransferModel model) {
		clear();
		// Set the model.
		this.model = model;
		
		// Update the mxGraph.
		graph = constructGraph(graph, model);
	}

	/*************************************************************
	 * 
	 */
	public boolean isValid() {
		if (model == null) return false;
		if (!Validation.checkUpdateConflict(model)) return false;
		return true;
	}
	
	/*************************************************************
	 * Construct a mxGraph from DataFlowModel
	 * @param model
	 * @param dataFlowGraph
	 * @return constructed mxGraph
	 */
	public mxGraph constructGraph(mxGraph graph, DataTransferModel model) {
		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell nodeLayer = (mxCell) root.getChildAt(NODE_LAYER);
		mxCell dataFlowLayer = (mxCell) root.getChildAt(DATA_FLOW_LAYER);
				
		graph.getModel().beginUpdate();
		try {
			mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo1.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo1.setRelative(true);

			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);

			Map<DataTransferChannelGenerator, Object> channelsIn = new HashMap<>();
			Map<DataTransferChannelGenerator, Object> channelsOut = new HashMap<>();
			Map<IdentifierTemplate, Object> resources = new HashMap<>();

			// create channel vertices
			for (ChannelGenerator c: model.getChannelGenerators()) {
				DataTransferChannelGenerator channelGen = (DataTransferChannelGenerator) c;
				if (channelsIn.get(channelGen) == null || channelsOut.get(channelGen) == null) {
					Object channel = graph.insertVertex(dataFlowLayer, null, channelGen.getChannelName(), 150, 20, 30, 30); // insert a channel as a vertex
					mxCell port_in = new mxCell(null, geo1, "shape=ellipse;perimter=ellipsePerimeter");
					port_in.setVertex(true);
					graph.addCell(port_in, channel);		// insert the input port of a channel					
					mxCell port_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
					port_out.setVertex(true);
					graph.addCell(port_out, channel);		// insert the output port of a channel
					channelsIn.put(channelGen, port_in);
					channelsOut.put(channelGen, port_out);
				}
			}

			// create resource vertices
			for (IdentifierTemplate res: model.getIdentifierTemplates()) {
				// insert a resource as a vertex
				ResourceNodeAttribute resNodeAttr = new ResourceNodeAttribute(model.getDataFlowGraph().getResouceNode(res));
				Object resource = graph.insertVertex(
						dataFlowLayer, null, resNodeAttr, 
						/*coordinate*/20, 20,
						/*    scale     */80, 30,
							   				  resNodeAttr.getDefaultStyle());
				resources.put(res, resource);
			}

			// add input, output and reference edges
			for (ChannelGenerator ch: model.getChannelGenerators()) {
				DataTransferChannelGenerator channelGen = (DataTransferChannelGenerator) ch;
				// input edge
				for (IdentifierTemplate srcRes: channelGen.getInputIdentifierTemplates()) {
					graph.insertEdge(dataFlowLayer, null, new SrcDstAttribute(srcRes, channelGen), resources.get(srcRes), channelsIn.get(channelGen), "movable=false;strokeColor=#FF0000");
				}
				// output edge
				for (IdentifierTemplate dstRes: channelGen.getOutputIdentifierTemplates()) {
					graph.insertEdge(dataFlowLayer, null, new SrcDstAttribute(channelGen, dstRes), channelsOut.get(channelGen), resources.get(dstRes), "movable=false;strokeColor=#FF0000");
				}
				// reference edges
				for (IdentifierTemplate refRes: channelGen.getReferenceIdentifierTemplates()) {
					graph.insertEdge(dataFlowLayer, null, null, resources.get(refRes), channelsIn.get(channelGen), "dashed=true;movable=false;strokeColor=#FF0000");
				}
			}

			for (ChannelGenerator ioChannelGen: model.getIOChannelGenerators()) {
				if (channelsOut.get(ioChannelGen) == null) {
					Object channel = graph.insertVertex(nodeLayer, null, ioChannelGen.getChannelName(), 150, 20, 30, 30); // insert an I/O channel as a vertex
					mxCell port_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
					port_out.setVertex(true);
					graph.addCell(port_out, channel);		// insert the output port of a channel
					channelsOut.put((DataTransferChannelGenerator) ioChannelGen, port_out);
					
					for (IdentifierTemplate outRes: ((DataTransferChannelGenerator) ioChannelGen).getOutputIdentifierTemplates()) {
						graph.insertEdge(dataFlowLayer, null, null, port_out, resources.get(outRes), "movable=false;strokeColor=#FF0000");
					}
					
				}
			}
		} finally {
			graph.getModel().endUpdate();
		}

		return graph;
	}

	/*************************************************************
	 * 
	 */
	public void addIdentifierTemplate(IdentifierTemplate res) {
		getModel().addIdentifierTemplate(res);
		graph.getModel().beginUpdate();
		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell layer = (mxCell) root.getChildAt(NODE_LAYER);
		try {
			graph.insertVertex(layer, null, res.getResourceName(), 20, 20, 80, 30,
					"shape=ellipse;perimeter=ellipsePerimeter"); // insert a resource as a vertex
		} finally {
			graph.getModel().endUpdate();
		}
	}

	/*************************************************************
	 * 
	 */
	public void addChannelGenerator(DataTransferChannelGenerator channelGen) {
		getModel().addChannelGenerator(channelGen);
		graph.getModel().beginUpdate();
		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell layer = (mxCell) root.getChildAt(DATA_FLOW_LAYER);
		try {
			mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo1.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo1.setRelative(true);
	
			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);
	
			Object channel = graph.insertVertex(layer, null, channelGen.getChannelName(), 150, 20, 30, 30); // insert a channel as a vertex
			mxCell port_in = new mxCell(null, geo1, "shape=ellipse;perimter=ellipsePerimeter");
			port_in.setVertex(true);
			graph.addCell(port_in, channel);		// insert the input port of a channel
			mxCell port_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
			port_out.setVertex(true);
			graph.addCell(port_out, channel);		// insert the output port of a channel
		} finally {
			graph.getModel().endUpdate();
		}
	}
	
	/*************************************************************
	 * 
	 */
	public void addIOChannelGenerator(DataTransferChannelGenerator ioChannelGen) {
		getModel().addIOChannelGenerator(ioChannelGen);
		graph.getModel().beginUpdate();
		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell layer = (mxCell) root.getChildAt(NODE_LAYER);
		try {
			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);
	
			Object channel = graph.insertVertex(layer, null, ioChannelGen.getChannelName(), 150, 20, 30, 30); // insert an I/O channel as a vertex
			mxCell port_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
			port_out.setVertex(true);
			graph.addCell(port_out, channel);		// insert the output port of a channel
		} finally {
			graph.getModel().endUpdate();
		}
	}

	public void addFormulaChannelGenerator(FormulaChannelGenerator formulaChannelGen) {
		getModel().addChannelGenerator(formulaChannelGen);
		graph.getModel().beginUpdate();
		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell layer = (mxCell) root.getChildAt(DATA_FLOW_LAYER);
		try {
			mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo1.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo1.setRelative(true);
	
			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);
	
			Object channel = graph.insertVertex(layer, null, formulaChannelGen.getChannelName(), 150, 20, 30, 30); // insert a channel as a vertex
			mxCell port_in = new mxCell(null, geo1, "shape=ellipse;perimter=ellipsePerimeter");
			port_in.setVertex(true);
			graph.addCell(port_in, channel);		// insert the input port of a channel
			mxCell port_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
			port_out.setVertex(true);
			graph.addCell(port_out, channel);		// insert the output port of a channel
		} finally {
			graph.getModel().endUpdate();
		}
	}

	public boolean connectEdge(mxCell edge, mxCell src, mxCell dst) {
		DataTransferModel model = getModel();
		ChannelGenerator srcCh = model.getChannelGenerator((String) src.getValue());
		if (srcCh == null) {
			srcCh = model.getIOChannelGenerator((String) src.getValue());
			if (srcCh == null) {
				IdentifierTemplate srcRes = model.getIdentifierTemplate((String) src.getValue());
				ChannelGenerator dstCh = model.getChannelGenerator((String) dst.getValue());
				if (srcRes == null || dstCh == null) return false;
				// resource to channel edge
				ChannelMember srcCm = new ChannelMember(srcRes);
				((DataTransferChannelGenerator ) dstCh).addChannelMemberAsInput(srcCm);
				edge.setValue(new SrcDstAttribute(srcRes, dstCh));
				return true;
			}
		}
		IdentifierTemplate dstRes = model.getIdentifierTemplate((String) dst.getValue());
		if (dstRes == null) return false;
		// channel to resource edge
		ChannelMember dstCm = new ChannelMember(dstRes);
		((DataTransferChannelGenerator) srcCh).addChannelMemberAsOutput(dstCm);
		edge.setValue(new SrcDstAttribute(srcCh, dstRes));
		return true;
	}

	/*************************************************************
	 * 
	 */
	public void delete() {
		for (Object obj: graph.getSelectionCells()) {
			mxCell cell = (mxCell) obj;
			if (cell.isEdge()) {
				String srcName = (String) cell.getSource().getValue();
				String dstName = (String) cell.getTarget().getValue();
				if (model.getIdentifierTemplate(srcName) != null) {
					// resource to channel edge
					ChannelGenerator ch = model.getChannelGenerator(dstName);
					ch.removeChannelMember(model.getIdentifierTemplate(srcName));
				} else if (model.getIdentifierTemplate(dstName) != null) {
					// channel to resource edge
					ChannelGenerator ch = model.getChannelGenerator(srcName);
					if (ch == null) {
						ch = model.getIOChannelGenerator(srcName);
					}
					ch.removeChannelMember(model.getIdentifierTemplate(dstName));
				}
			} else if (cell.isVertex()) {
				String name = (String) cell.getValue();
				if (model.getChannelGenerator(name) != null) {
					model.removeChannelGenerator(name);
				} else if (model.getIOChannelGenerator(name) != null) {
					model.removeIOChannelGenerator(name);
				} else if (model.getIdentifierTemplate(name) != null) {
					model.removeIdentifierTemplate(name);
				}
			}
		}
		graph.removeCells(graph.getSelectionCells());
	}

	/*************************************************************
	 * 
	 */
	public void setChannelCode(DataTransferChannelGenerator ch, String code) {
		ch.setSourceText(code);
		TokenStream stream = new TokenStream();
		Parser parser = new Parser(stream);
		
		for (String line: code.split("\n")) {
			stream.addLine(line);
		}
		try {
			DataTransferChannelGenerator ch2 = parser.parseChannel(getModel());
			for (ChannelMember chm2: ch2.getInputChannelMembers()) {
				for (ChannelMember chm: ch.getInputChannelMembers()) {
					if (chm2.getIdentifierTemplate() == chm.getIdentifierTemplate()) {
						chm.setStateTransition(chm2.getStateTransition());
						break;
					}
				}
			}
			for (ChannelMember chm2: ch2.getOutputChannelMembers()) {
				for (ChannelMember chm: ch.getOutputChannelMembers()) {
					if (chm2.getIdentifierTemplate() == chm.getIdentifierTemplate()) {
						chm.setStateTransition(chm2.getStateTransition());
						break;
					}
				}
			}
			for (ChannelMember chm2: ch2.getReferenceChannelMembers()) {
				for (ChannelMember chm: ch.getReferenceChannelMembers()) {
					if (chm2.getIdentifierTemplate() == chm.getIdentifierTemplate()) {
						chm.setStateTransition(chm2.getStateTransition());
						break;
					}
				}
			}
		} catch (ExpectedRightBracket | ExpectedChannel | ExpectedChannelName | ExpectedLeftCurlyBracket
				| ExpectedInOrOutOrRefKeyword | ExpectedStateTransition | ExpectedEquals | ExpectedRHSExpression
				| WrongLHSExpression | WrongRHSExpression | ExpectedAssignment e) {
			e.printStackTrace();
		}
	}
}
