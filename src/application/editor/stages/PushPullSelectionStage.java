package application.editor.stages;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;

import algorithms.DataTransferModelAnalyzer;
import application.editor.Editor.SrcDstAttribute;
import application.editor.Editor;
import application.editor.FlowCellEditor;
import application.editor.InEdgeAttribute;
import application.editor.OutEdgeAttribute;
import application.editor.Stage;
import models.Edge;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataFlowGraph;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.ResourceNode;

public class PushPullSelectionStage extends Stage {
	protected DataFlowGraph dataFlowGraph = null;
	
	public PushPullSelectionStage(mxGraphComponent graphComponent) {
		super(graphComponent);
	}

	@Override
	public boolean canChangeFrom(Stage prevStage) {
		if (prevStage instanceof DataFlowModelingStage) {
			if (!((DataFlowModelingStage) prevStage).isValid()) return false;
			return true;
		}
		if (prevStage instanceof ControlFlowDelegationStage) return true;
		return false;
	}
	
	@Override
	public void init(Stage prevStage) {
		if (prevStage instanceof DataFlowModelingStage) {
			model = ((DataFlowModelingStage) prevStage).getModel();
			dataFlowGraph = analyzeDataTransferModel(graph, model);
			showOnlyLayer(DATA_FLOW_LAYER);
		}
		
		if(prevStage instanceof ControlFlowDelegationStage) {
			showOnlyLayer(DATA_FLOW_LAYER);
		}
	}

	@Override
	public FlowCellEditor createCellEditor(mxGraphComponent graphComponent) {
		return new PushPullSelectionCellEditor(this, graphComponent);
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
					// cancel connect
					graph.removeCells(new mxCell[] {cell});
				}
			}
		};
	}
	
	@Override
	public MouseListener createMouseEventListener(Editor editor) {
		return null;
	}

	
	public DataFlowGraph getDataFlowGraph() {
		return dataFlowGraph;
	}
	
	public DataFlowGraph analyzeDataTransferModel(mxGraph graph, DataTransferModel model) {
		DataFlowGraph flowGraph = DataTransferModelAnalyzer.createDataFlowGraphWithStateStoringAttribute(model);
		DataFlowGraph dataFlowGraph = DataTransferModelAnalyzer.annotateWithSelectableDataTransferAttiribute(flowGraph);
		updateEdgeAttiributes(graph, dataFlowGraph);
		return dataFlowGraph;
	}

	private void updateEdgeAttiributes(mxGraph graph, DataFlowGraph dataFlowGraph) {
		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell layer = (mxCell) root.getChildAt(DATA_FLOW_LAYER);

		graph.getModel().beginUpdate();
		try {
			// update attributes of input and output edges
			for (Edge e : dataFlowGraph.getEdges()) {
				if (e instanceof DataFlowEdge) {
					DataFlowEdge dataFlow = (DataFlowEdge) e;
					DataTransferChannel channel = dataFlow.getChannel();
					ResourceNode srcRes = (ResourceNode) dataFlow.getSource();
					ResourceNode dstRes = (ResourceNode) dataFlow.getDestination();
					for (Object edge: graph.getChildEdges(layer)) {
						mxCell edgeCell = (mxCell) edge;
						if (edgeCell.getValue() instanceof SrcDstAttribute) {
							SrcDstAttribute edgeAttr = (SrcDstAttribute) edgeCell.getValue();
							if (edgeAttr.getSrouce() == srcRes.getResource() && edgeAttr.getDestination() == channel) {
								// input edge
								edgeCell.setValue(new InEdgeAttribute((PushPullAttribute) dataFlow.getAttribute(), srcRes));
								break;
							} else if (edgeAttr.getSrouce() == channel && edgeAttr.getDestination() == dstRes.getResource()) {
								// output edge
								edgeCell.setValue(new OutEdgeAttribute(edgeAttr, dstRes));
								break;								
							}
						}
					}
				}
			}
		} finally {
			graph.getModel().endUpdate();
		}
		graph.refresh();
	}
}
