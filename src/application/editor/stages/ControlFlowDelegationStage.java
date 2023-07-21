package application.editor.stages;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxMouseAdapter;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import application.editor.Editor;
import application.editor.FlowCellEditor;
import application.editor.Stage;
import application.views.PopupMenuBase;
import application.views.controlFlowDelegation.ControlFlowDelegationStagePopupMenu;
import models.Edge;
import models.Node;
import models.controlFlowModel.CallEdge;
import models.controlFlowModel.CallEdgeAttribute;
import models.controlFlowModel.CallGraph;
import models.controlFlowModel.ControlFlowDelegator;
import models.controlFlowModel.ControlFlowGraph;
import models.controlFlowModel.EntryPointObjectNode;
import models.controlFlowModel.ObjectNode;
import models.controlFlowModel.ObjectNodeAttribute;
import models.controlFlowModel.StatefulObjectNode;
import models.dataFlowModel.DataFlowGraph;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.ResourceNodeAttribute;

/*************************************************************
 * 
 */
public class ControlFlowDelegationStage extends Stage {
	public final int PORT_DIAMETER = 8;
	public final int PORT_RADIUS = PORT_DIAMETER / 2;

	private ControlFlowDelegationStageStatus curState = null;

	private ControlFlowGraph controlFlowGraph = null;
	private PopupMenuBase popupMenu = null;

	/*************************************************************
	 *  [ *constructor ]
	/*************************************************************
	 * 
	 */
	public ControlFlowDelegationStage(mxGraphComponent graphComoponent) {
		super(graphComoponent);
		this.curState = ControlFlowDelegationStageStatus.SELECTING_AN_EDGE;
		this.popupMenu = new ControlFlowDelegationStagePopupMenu(this, graphComoponent);
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * getters / setters
	/*************************************************************
	 */
	public ControlFlowGraph getControlFlowGraph() {
		return controlFlowGraph;
	}

	/*************************************************************
	 * 
	 */
	public ControlFlowDelegationStageStatus getCurState() {
		return curState;
	}
	
	public void setState(ControlFlowDelegationStageStatus nextState) {
		curState = nextState;
	}

	/*************************************************************
	 * 
	 */
	@Override
	public boolean canChangeFrom(Stage prevStage) {
		if (prevStage instanceof PushPullSelectionStage) return true;
		return false;
	}

	/*************************************************************
	 * 
	 */
	@Override
	public void init(Stage prevStage) {
		if (prevStage instanceof PushPullSelectionStage) {
			model = ((PushPullSelectionStage) prevStage).getModel();

			DataFlowGraph dataFlowGraph = ((PushPullSelectionStage) prevStage).getDataFlowGraph();

			controlFlowGraph = new ControlFlowGraph(dataFlowGraph, model);

			clearControlFlowGraphCells(graph);
			graph = constructGraph(graph, controlFlowGraph);
		}
	}

	
	/*************************************************************
	 * 
	 */
	@Override
	public FlowCellEditor createCellEditor(mxGraphComponent graphComponent) {
		return new ControlFlowDelegationCellEditor(this, graphComponent);
	}

	/*************************************************************
	 * 
	 */
	@Override
	public mxIEventListener createChangeEventListener(Editor editor) {	
		return null;
	}

	/*************************************************************
	 * 
	 */
	@Override
	public MouseListener createMouseEventListener(Editor editor) {
		MouseListener listener =  new mxMouseAdapter() {	
			@Override
			public void mouseReleased(MouseEvent e) {
				if(SwingUtilities.isLeftMouseButton(e)) {
					if(graphComponent.getCellAt(e.getX(), e.getY()) != null) return;
					if(curState.equals(ControlFlowDelegationStageStatus.SELECTING_AN_EDGE)) return;

					System.out.println("cancel showing state.");
					resetAllStyleOfCells();
					curState = ControlFlowDelegationStageStatus.SELECTING_AN_EDGE;
					return;
				}
				else 	if(SwingUtilities.isRightMouseButton(e)) {
					popupMenu.show(e.getX(), e.getY());					
				}
			}
		};
		return listener;
	}


	/*************************************************************
	 * manipulating the control-graph
	/*************************************************************
	 * 
	 */
	public void  showDelegatableNodes(mxGraph graph, final CallEdgeAttribute callEdgeAttr){
		mxCell root = (mxCell)graph.getDefaultParent();
		graph.getModel().beginUpdate();	
		try {
			ObjectNode delegatingNode = callEdgeAttr.getDestinationObjectNode();
			for(int layerNo = Stage.PUSH_FLOW_LAYER; layerNo <= PULL_FLOW_LAYER; layerNo++) {

				mxCell layerCell = (mxCell)root.getChildAt(layerNo);
				for(Object node : graph.getChildVertices(layerCell)) {
					if( !(node instanceof mxCell) ) continue;
					mxCell cell = (mxCell)node;

					ObjectNodeAttribute objNodeAttr = (ObjectNodeAttribute)cell.getValue();
					if(objNodeAttr == null) return;

					ObjectNode objNode = objNodeAttr.getObjectNode();				
					ControlFlowDelegator delegator = new ControlFlowDelegator(controlFlowGraph);
					List<ObjectNode> delegatableNodes = delegator.searchDelegatableNodes(callEdgeAttr.getCallEdge());

					if(delegatableNodes.contains(objNode)) {
						graph.getModel().setStyle(cell, objNodeAttr.getEnableStyle());
					}
					else {
						graph.getModel().setStyle(cell, objNodeAttr.getDisableStyle());
					}

					if(delegatingNode.equals(objNodeAttr.getObjectNode()))
						/* base-Node*/graph.getModel().setStyle(cell, objNodeAttr.getDelegatingStyle());			
				}
			}
		}
		finally {
			graph.getModel().endUpdate();
			graph.refresh();
		}
	}

	/*************************************************************
	 * Showing the graph that was executed CFD.
	 */
	public void showDelegatedGraph(mxGraph graph, mxCell targetEdgeCell, final mxCell dstObjNodeCell) {
		ObjectNode dstObjNode = ((ObjectNodeAttribute)dstObjNodeCell.getValue()).getObjectNode();
		if(dstObjNode == null) throw new ClassCastException();
		CallEdgeAttribute targetEdgeAttr = (CallEdgeAttribute)targetEdgeCell.getValue();
		if(targetEdgeAttr == null) throw new ClassCastException();

		ControlFlowDelegator delegator = new ControlFlowDelegator(controlFlowGraph);
		delegator.delegateCallEdge(targetEdgeAttr.getCallEdge(), dstObjNode);

		mxCell root = (mxCell)graph.getDefaultParent();
		mxCell layerCell = null;
		switch(targetEdgeAttr.getSelectedOption()) {
		case PUSH:
			layerCell = (mxCell)root.getChildAt(Stage.PUSH_FLOW_LAYER);
			break;

		case PULL:
		case PUSHorPULL:
			layerCell = (mxCell)root.getChildAt(Stage.PULL_FLOW_LAYER);
		}

		try {
			mxCell dstNodeCell = targetEdgeAttr.getDestinationCell();

			// Removing the target edge from graph model.
			if(graph.getModel().getValue(targetEdgeCell) != null) {
				graph.getModel().remove(targetEdgeCell);
			}

			// Insert an edge
			CallEdgeAttribute newAttr = new CallEdgeAttribute(targetEdgeAttr.getCallEdge(), targetEdgeAttr.getOriginalSourceObjectNode(),dstObjNodeCell, dstNodeCell);
			graph.insertEdge(layerCell, "",  newAttr, dstObjNodeCell, dstNodeCell, "movable=false;");

			resetAllStyleOfCells();
		}
		finally {
			graph.getModel().endUpdate();
		}
	}

	/*************************************************************
	 * 
	 */
	public void resetAllStyleOfCells() {
		mxCell root = (mxCell)graph.getDefaultParent();

		graph.getModel().beginUpdate();
		try {
			for(int layerNo = Stage.PUSH_FLOW_LAYER; layerNo <= Stage.PULL_FLOW_LAYER; layerNo++) {

				mxCell layerCell = (mxCell)root.getChildAt(layerNo);
				for(Object node : graph.getChildVertices(layerCell)) {				
					mxCell cell = null;
					if(node instanceof mxCell)	cell = (mxCell)node;
					else continue;

					ObjectNodeAttribute objNodeAttr = (ObjectNodeAttribute)(cell.getValue());				
					if(objNodeAttr == null)	throw new NullPointerException("");

					graph.getModel().setStyle(cell, objNodeAttr.getDefaultStyle());
				}				
			}
		}
		finally {
			graph.getModel().endUpdate();
			graph.refresh();
		}
	}

	/*************************************************************
	 * Inserting an intermediation object type of <Object-Node>.
	 */
	public void insertObjectNodeCellInControlFlowLayer(mxGraph graph, mxCell targetEdge, final String insertObjName) {		
		CallEdgeAttribute callEdgeAttr = (CallEdgeAttribute)targetEdge.getValue();
		if(callEdgeAttr == null) throw new NullPointerException();

		mxCell root = (mxCell)graph.getDefaultParent();
		mxCell layerCell = null;
		switch(callEdgeAttr.getSelectedOption()) {
		case PUSH:
			layerCell = (mxCell)root.getChildAt(Stage.PUSH_FLOW_LAYER);
			break;

		case PULL:
		case PUSHorPULL:
			layerCell = (mxCell)root.getChildAt(Stage.PULL_FLOW_LAYER);
		}

		graph.getModel().beginUpdate();

		try {
			// Inserting the node type of <Object Node> to the graph.
			ObjectNode insertObjNode = new ObjectNode(insertObjName);
			ObjectNodeAttribute objNodeAttr = new ObjectNodeAttribute(insertObjNode);

			mxPoint srcPoint = new mxPoint(callEdgeAttr.getSourceCell().getGeometry().getX(), callEdgeAttr.getSourceCell().getGeometry().getY());						
			mxPoint dstPoint = new mxPoint(callEdgeAttr.getDestinationCell().getGeometry().getX(), callEdgeAttr.getDestinationCell().getGeometry().getY());
			mxPoint insertPoint = new mxPoint( 
					(srcPoint.getX() + dstPoint.getX())/2,
					(srcPoint.getY() + dstPoint.getY())/2);

			mxCell insertObjNodeCell = 
					(mxCell)graph.insertVertex(layerCell, null, objNodeAttr, 
							/* coordinate*/ insertPoint.getX(), insertPoint.getY(),
							/*     scale     */ 40, 40,
							objNodeAttr.getDefaultStyle());
			insertObjNodeCell.setValue(objNodeAttr);

			addObjectNodeToCallGraphl(insertObjNode, callEdgeAttr.getSelectedOption());

			// Reconnecting each edges of the node.
			ObjectNode srcObjNode = callEdgeAttr.getSourceObjectNode();
			ObjectNode dstObjNode = callEdgeAttr.getDestinationObjectNode();
			if(srcObjNode == null || dstObjNode == null) throw new NullPointerException();
			if(!(srcObjNode instanceof ObjectNode && dstObjNode instanceof ObjectNode)) throw new ClassCastException();

			// Connecting I/O Edges to the insert object.
			CallEdge srcToInsertEdge = callEdgeAttr.getCallEdge();	
			CallEdge insertToDstEdge = new CallEdge(insertObjNode, dstObjNode, callEdgeAttr.getSelectedOption());			
			if(srcToInsertEdge == null || insertToDstEdge == null) throw new NullPointerException();

			// Remove the destination edge of the object node.
			// After add the "srcToInsertEdge" to the destination object node.
			dstObjNode.removeInEdge(srcToInsertEdge);
			dstObjNode.addInEdge(insertToDstEdge);

			srcToInsertEdge.setDestination(insertObjNode); // changing the out of edge of the sourceObjectNode

			insertObjNode.addInEdge(srcToInsertEdge);
			insertObjNode.addOutEdge(insertToDstEdge);

			// Update the cell of the graph.
			for(int i  =0; i < layerCell.getChildCount(); i++) {
				mxCell nodeCell = (mxCell)layerCell.getChildAt(i);
				if( !nodeCell.isVertex()) continue;

				// Checking "nodeCell" has an instance of <ObjectNodeAttribute>
				ObjectNodeAttribute cellObjNodeAttr = (ObjectNodeAttribute)nodeCell.getValue();
				if(cellObjNodeAttr == null) throw new ClassCastException("dosen't have the value of <ObjectNodeAttribute>");

				//  Is "nodeCell" the same as the source cell of the call edge?
				if(nodeCell.equals(callEdgeAttr.getSourceCell())){
					mxCell srcNodeCell = callEdgeAttr.getSourceCell();
					CallEdgeAttribute newInEdgeAttr = new CallEdgeAttribute(srcToInsertEdge, srcNodeCell, insertObjNodeCell);

					// If the target call edge hasn't removed yet.
					// then it removes from mxGraphModel.
					if(graph.getModel().getValue(targetEdge) != null)
						graph.getModel().remove(targetEdge);

					mxCell outPortCell = (mxCell)srcNodeCell.getChildAt(0);
					if(outPortCell != null) {
						graph.insertEdge(layerCell, null,  newInEdgeAttr, outPortCell, insertObjNodeCell, "movable=false;");	
					}
					else {
						graph.insertEdge(layerCell, null,  newInEdgeAttr, srcNodeCell, insertObjNodeCell, "movable=false;");	
					}
					continue;
				}
				//  Is "nodeCell" the same as the destination cell of the call edge?
				else if(nodeCell.equals(callEdgeAttr.getDestinationCell())) {
					mxCell dstNodeCell = callEdgeAttr.getDestinationCell();
					CallEdgeAttribute newOutEdgeAttr = new CallEdgeAttribute(insertToDstEdge, insertObjNodeCell, dstNodeCell);

					// If the target 
					if(graph.getModel().getValue(targetEdge) != null) 
						graph.getModel().remove(targetEdge);

					graph.insertEdge(layerCell, null,  newOutEdgeAttr, insertObjNodeCell, dstNodeCell, "movable=false;");								

					continue;
				}
			}
		}
		finally {
			graph.getModel().endUpdate();
		}
	}


	/*************************************************************
	 * 
	 */
	public boolean isExecutableDelegation(final CallEdgeAttribute targetEdgeAttr, final ObjectNode dstObjNode) {
		ControlFlowDelegator delegator = new ControlFlowDelegator(controlFlowGraph);
		List<ObjectNode> delegatableNodes = delegator.searchDelegatableNodes(targetEdgeAttr.getCallEdge());

		return delegatableNodes.contains(dstObjNode);
	}


	/*************************************************************
	 * [ *private ]
	/*************************************************************
	 * 
	 */
	private mxGraph constructGraph(mxGraph graph, ControlFlowGraph controlFlowGraph) {				
		showOnlyLayer(PUSH_FLOW_LAYER, PULL_FLOW_LAYER);

		graph.getModel().beginUpdate();
		try {
			// Creating Control-Flow and separeted Push/Pull <mxCell> which types of <ResourceNode>
			Map<ResourceNode, mxCell> pushResNodeCells = createCellsOfResourceMap(graph, PUSH_FLOW_LAYER, controlFlowGraph);
			Map<ResourceNode, mxCell> pullResNodeCells = createCellsOfResourceMap(graph, PULL_FLOW_LAYER, controlFlowGraph);

			// Creating Entry-Point Object
			Map<EntryPointObjectNode, mxCell> pushFlowEntryNodeCells = createCellsOfInputChannel(graph, PUSH_FLOW_LAYER, controlFlowGraph, pushResNodeCells);

			// Inserting edges of each transfer
			graph = insertControlFlowEdges(graph, PUSH_FLOW_LAYER, controlFlowGraph.getPushCallGraph(), pushResNodeCells, pushFlowEntryNodeCells);
			graph = insertControlFlowEdges(graph, PULL_FLOW_LAYER, controlFlowGraph.getPullCallGraph(), pullResNodeCells, null);			

		} finally {
			graph.getModel().endUpdate();
		}		
		return graph;
	}

	
	/*************************************************************
	 * When changed from previous stage, it will be called in initializing.
	 */
	private void clearControlFlowGraphCells(mxGraph graph) {
		mxCell root = (mxCell)graph.getDefaultParent();

		graph.getModel().beginUpdate();
		try {
			// removing child from end of a root cell
			root.remove(root.getChildAt(PULL_FLOW_LAYER));
			root.remove(root.getChildAt(PUSH_FLOW_LAYER));

			root.insert(new mxCell());
			root.insert(new mxCell());	
		} finally {
			graph.getModel().endUpdate();
			graph.refresh();
		}
	}

	
	/*************************************************************
	 * Creating a map of <ResourceNode> to <Object(mxCell)> and Creating <ResourceNode>'s vertices
	 * @return constructed the view of the graph
	 */
	private Map<ResourceNode, mxCell> createCellsOfResourceMap(mxGraph graph, final int layerNumber, final ControlFlowGraph controlFlowGraph) {
		Map<ResourceNode, mxCell> resNodeCells = new HashMap<>();

		mxCell root = (mxCell)graph.getDefaultParent();
		mxCell nodeLayerCell = (mxCell)root.getChildAt(NODE_LAYER);
		mxCell layerCell = (mxCell)root.getChildAt(layerNumber);

		// create resource vertices
		for (ResourceNode resNode : controlFlowGraph.getDataFlowGraph().getResouceNodes()) {

			ObjectNode objNode = null;
			switch(layerNumber) {
			case PUSH_FLOW_LAYER:
				if(controlFlowGraph.getPushCallGraph().getStatefulObjectNode(resNode) != null)
					objNode = controlFlowGraph.getPushCallGraph().getStatefulObjectNode(resNode);
				break;

			case PULL_FLOW_LAYER:
				if(controlFlowGraph.getPullCallGraph().getStatefulObjectNode(resNode) != null)
					objNode = controlFlowGraph.getPullCallGraph().getStatefulObjectNode(resNode);						
				break;
			}

			if(objNode == null) continue;

			for(int i  =0; i < nodeLayerCell.getChildCount(); i++) {
				mxCell nodeCell = (mxCell)nodeLayerCell.getChildAt(i);
				if( nodeCell.getValue() instanceof ResourceNodeAttribute ) {
					nodeCell = (mxCell)nodeLayerCell.getChildAt(i);
				}
				else continue;

				// Checking if the "node" has a cell of the data-flow-layer is the same as "resNode".
				ResourceNodeAttribute resNodeAttr = (ResourceNodeAttribute)nodeCell.getValue();
				if( !resNodeAttr.getResourceNode().equals(resNode) )continue;

				// Getting information from the cell in the data-flow-layer,
				//  After that, insert a resource as a vertex
				ObjectNodeAttribute objNodeAttr = new ObjectNodeAttribute(objNode);
				mxCell resNodeObjCell = (mxCell)graph.insertVertex(layerCell, null, objNodeAttr,
						/*     scale    */nodeCell.getGeometry().getX(), nodeCell.getGeometry().getY(),
						/*coordinate*/nodeCell.getGeometry().getWidth(), nodeCell.getGeometry().getHeight(),
						objNodeAttr.getDefaultStyle());

				resNodeCells.put(resNode, resNodeObjCell);
			}			
		}

		return resNodeCells;
	}


	/*************************************************************
	 * Createing an input channel object
	 */
	private Map<EntryPointObjectNode, mxCell> createCellsOfInputChannel(mxGraph graph, final int layerNumber, final ControlFlowGraph controlGraph, final Map<ResourceNode, mxCell> resNodeCell){
		if(layerNumber == PULL_FLOW_LAYER) return null;

		mxCell root = (mxCell)graph.getDefaultParent();
		mxCell layerCell = (mxCell)root.getChildAt(layerNumber);

		Map<EntryPointObjectNode, mxCell> ioChannelCells = new HashMap<>();

		graph.getModel().beginUpdate();
		try {
			mxGeometry outPortGeometry = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			outPortGeometry.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			outPortGeometry.setRelative(true);

			CallGraph callGraph = controlFlowGraph.getPushCallGraph();

			// insert an I/O channel as a vertex	
			for (Node node : callGraph.getNodes()) {
				EntryPointObjectNode entryPointObjNode = null;
				if(node instanceof EntryPointObjectNode) 
					entryPointObjNode = (EntryPointObjectNode)node;
				else continue;

				ObjectNodeAttribute entryObjAttr = new ObjectNodeAttribute(entryPointObjNode);

				// Taking over geometry information from the channel node with the same name.
				mxCell dataFlowLayerCell = (mxCell)root.getChildAt(Stage.DATA_FLOW_LAYER);
				for(int i = 0; i < dataFlowLayerCell.getChildCount(); i++) {
					mxCell channelCell =(mxCell)dataFlowLayerCell.getChildAt(i);

					String entryPointObjNodeName = entryPointObjNode.getIoChannelGenerator().getChannelName();
					String channelCellName = "";
					if(channelCell.getValue() instanceof String) channelCellName = (String) channelCell.getValue();
					else continue;

					if(!entryPointObjNodeName.equals(channelCellName))continue;

					mxCell entryPointCelll = (mxCell)graph.insertVertex(layerCell, null, entryObjAttr, 
							/*     scale    */ channelCell.getGeometry().getX(), channelCell.getGeometry().getY(), 
							/*  geometry*/channelCell.getGeometry().getWidth(), channelCell.getGeometry().getHeight()); 
					mxCell port_out = new mxCell(null, outPortGeometry, "shape=ellipse;perimter=ellipsePerimeter");
					port_out.setVertex(true);

					graph.addCell(port_out, entryPointCelll);		// insert the output port of a channel
					ioChannelCells.put(entryPointObjNode, entryPointCelll);
				}
			}
		}
		finally {
			graph.getModel().endUpdate();
		}

		return ioChannelCells;
	}


	/*************************************************************
	 * When changed from previous stage, it will be called in initializing.
	 */
	private mxGraph insertControlFlowEdges(mxGraph graph, final int layerNumber, final CallGraph callGraph ,final Map<ResourceNode, mxCell> resNodeCells, final Map<EntryPointObjectNode, mxCell> entryNodeCells) {
		mxCell root = (mxCell)graph.getDefaultParent();
		mxCell layerCell = (mxCell)root.getChildAt(layerNumber);

		for (Edge callGraphEdge : callGraph.getEdges()) {
			if ( !(callGraphEdge instanceof CallEdge))continue;
			CallEdge callEdge = (CallEdge) callGraphEdge;

			// Is checking node connecting a resource?
			if(callEdge.getSource() == null || callEdge.getDestination() == null) continue;

			Node srcResNode = null;
			ResourceNode dstResNode = ((StatefulObjectNode)callEdge.getDestination()).getResource();

			mxCell srcNodeCell =null;
			mxCell srcOutPortCell = null;
			mxCell dstNodeCell = resNodeCells.get(dstResNode);

			if(callEdge.getSource() instanceof StatefulObjectNode) {
				srcResNode = ((StatefulObjectNode)callEdge.getSource()).getResource();
				srcNodeCell =resNodeCells.get(srcResNode);
			}
			else if (callEdge.getSource() instanceof EntryPointObjectNode) {
				srcResNode = (EntryPointObjectNode)callEdge.getSource();
				srcNodeCell = entryNodeCells.get(srcResNode);
				srcOutPortCell = (mxCell)srcNodeCell.getChildAt(0);
			}
			else continue;

			if(srcNodeCell == null || dstNodeCell == null) continue;

			CallEdgeAttribute callEdgeAttr = new CallEdgeAttribute(callEdge, (ObjectNode)callEdge.getSource(), srcNodeCell, dstNodeCell);

			// If "srcResNode" types of "EntryPointObjectNode" (= channel)
			// then parameter references to geometry of "outPort". 
			if(srcResNode instanceof ResourceNode) {
				graph.insertEdge(layerCell, null,  callEdgeAttr, srcNodeCell, dstNodeCell, "movable=false;");
			}
			else if(srcResNode instanceof EntryPointObjectNode) {
				graph.insertEdge(layerCell, null,  callEdgeAttr, srcOutPortCell, dstNodeCell, "movable=false;");
			}

		}
		return graph;
	}

	
	/*************************************************************
	 * 
	 */
	private void addObjectNodeToCallGraphl(final ObjectNode insertObjNode, final PushPullValue selectedOption) {
		switch(selectedOption) {
		case PUSH:
			if(controlFlowGraph.getPushCallGraph().getNodes().contains(insertObjNode))return;
			controlFlowGraph.getPushCallGraph().addNode(insertObjNode);

			break;

		case PULL:
		case PUSHorPULL:
			if(controlFlowGraph.getPullCallGraph().getNodes().contains(insertObjNode))return;
			controlFlowGraph.getPullCallGraph().addNode(insertObjNode);

			break;
		}
	}


}
