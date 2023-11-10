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
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.ResourceNodeAttribute;
import models.visualModel.FormulaChannel;
import parser.Parser;
import parser.Parser.TokenStream;
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

/*************************************************************
 * 
 * @author n-nitta, k-fujii
 */
public class DataFlowModelingStage extends Stage {
	public int PORT_DIAMETER = 8;
	public int PORT_RADIUS = PORT_DIAMETER / 2;
	
	private boolean bReflectingArchitectureModel = false;
	
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
		bReflectingArchitectureModel = true;
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

			Map<DataTransferChannel, Object> channelsIn = new HashMap<>();
			Map<DataTransferChannel, Object> channelsOut = new HashMap<>();
			Map<ResourcePath, Object> resources = new HashMap<>();

			// create channel vertices
			for (Channel c: model.getChannels()) {
				DataTransferChannel channelGen = (DataTransferChannel) c;
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
			for (ResourcePath res: model.getResourcePaths()) {
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
			for (Channel ch: model.getChannels()) {
				DataTransferChannel channel = (DataTransferChannel) ch;
				// input edge
				for (ResourcePath srcRes: channel.getInputResources()) {
					graph.insertEdge(dataFlowLayer, null, new SrcDstAttribute(srcRes, channel), resources.get(srcRes), channelsIn.get(channel), "movable=false;strokeColor=#FF0000");
				}
				// output edge
				for (ResourcePath dstRes: channel.getOutputResources()) {
					graph.insertEdge(dataFlowLayer, null, new SrcDstAttribute(channel, dstRes), channelsOut.get(channel), resources.get(dstRes), "movable=false;strokeColor=#FF0000");
				}
				// reference edges
				for (ResourcePath refRes: channel.getReferenceResources()) {
					graph.insertEdge(dataFlowLayer, null, null, resources.get(refRes), channelsIn.get(channel), "dashed=true;movable=false;strokeColor=#FF0000");
				}
			}

			for (Channel ioChannel: model.getIOChannels()) {
				if (channelsOut.get(ioChannel) == null) {
					Object channel = graph.insertVertex(nodeLayer, null, ioChannel.getChannelName(), 150, 20, 30, 30); // insert an I/O channel as a vertex
					mxCell port_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
					port_out.setVertex(true);
					graph.addCell(port_out, channel);		// insert the output port of a channel
					channelsOut.put((DataTransferChannel) ioChannel, port_out);
					
					for (ResourcePath outRes: ((DataTransferChannel) ioChannel).getOutputResources()) {
						graph.insertEdge(dataFlowLayer, null, null, port_out, resources.get(outRes), "movable=false;strokeColor=#FF0000");
					}
					
				}
			}
		} finally {
			graph.getModel().endUpdate();
		}

		bReflectingArchitectureModel = false;
		return graph;
	}

	/*************************************************************
	 * 
	 */
	public void addResourcePath(ResourcePath res) {
		getModel().addResourcePath(res);
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
	public void addChannel(DataTransferChannel channelGen) {
		getModel().addChannel(channelGen);
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
	public void addIOChannel(DataTransferChannel ioChannelGen) {
		getModel().addIOChannel(ioChannelGen);
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

	public void addFormulaChannel(FormulaChannel formulaChannelGen) {
		getModel().addChannel(formulaChannelGen);
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
		if (bReflectingArchitectureModel) return false;
		DataTransferModel model = getModel();
		Channel srcCh = model.getChannel((String) src.getValue());
		if (srcCh == null) {
			srcCh = model.getIOChannel((String) src.getValue());
			if (srcCh == null) {
				ResourcePath srcRes = model.getResourcePath((String) src.getValue());
				Channel dstCh = model.getChannel((String) dst.getValue());
				if (srcRes == null || dstCh == null) return false;
				// resource to channel edge
				ChannelMember srcCm = new ChannelMember(srcRes);
				((DataTransferChannel ) dstCh).addChannelMemberAsInput(srcCm);
				edge.setValue(new SrcDstAttribute(srcRes, dstCh));
				return true;
			}
		}
		ResourcePath dstRes = model.getResourcePath((String) dst.getValue());
		if (dstRes == null) return false;
		// channel to resource edge
		ChannelMember dstCm = new ChannelMember(dstRes);
		((DataTransferChannel) srcCh).addChannelMemberAsOutput(dstCm);
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
				if (model.getResourcePath(srcName) != null) {
					// resource to channel edge
					Channel ch = model.getChannel(dstName);
					ch.removeChannelMember(model.getResourcePath(srcName));
				} else if (model.getResourcePath(dstName) != null) {
					// channel to resource edge
					Channel ch = model.getChannel(srcName);
					if (ch == null) {
						ch = model.getIOChannel(srcName);
					}
					ch.removeChannelMember(model.getResourcePath(dstName));
				}
			} else if (cell.isVertex()) {
				String name = (String) cell.getValue();
				if (model.getChannel(name) != null) {
					model.removeChannel(name);
				} else if (model.getIOChannel(name) != null) {
					model.removeIOChannel(name);
				} else if (model.getResourcePath(name) != null) {
					model.removeResourcePath(name);
				}
			}
		}
		graph.removeCells(graph.getSelectionCells());
	}

	/*************************************************************
	 * 
	 */
	public void setChannelCode(DataTransferChannel ch, String code) {
		ch.setSourceText(code);
		TokenStream stream = new TokenStream();
		Parser parser = new Parser(stream);
		
		for (String line: code.split("\n")) {
			stream.addLine(line);
		}
		try {
			DataTransferChannel ch2 = parser.parseChannel(getModel());
			for (ChannelMember chm2: ch2.getInputChannelMembers()) {
				for (ChannelMember chm: ch.getInputChannelMembers()) {
					if (chm2.getResource() == chm.getResource()) {
						chm.setStateTransition(chm2.getStateTransition());
						break;
					}
				}
			}
			for (ChannelMember chm2: ch2.getOutputChannelMembers()) {
				for (ChannelMember chm: ch.getOutputChannelMembers()) {
					if (chm2.getResource() == chm.getResource()) {
						chm.setStateTransition(chm2.getStateTransition());
						break;
					}
				}
			}
			for (ChannelMember chm2: ch2.getReferenceChannelMembers()) {
				for (ChannelMember chm: ch.getReferenceChannelMembers()) {
					if (chm2.getResource() == chm.getResource()) {
						chm.setStateTransition(chm2.getStateTransition());
						break;
					}
				}
			}
		} catch (ExpectedRightBracket | ExpectedChannel | ExpectedChannelName | ExpectedLeftCurlyBracket
				| ExpectedInOrOutOrRefKeyword | ExpectedStateTransition | ExpectedEquals | ExpectedRHSExpression
				| WrongLHSExpression | WrongRHSExpression | ExpectedAssignment | WrongJsonExpression | ExpectedColon e) {
			e.printStackTrace();
		}
	}
}
