package application.editor.stages;

import java.util.EventObject;

import javax.swing.JOptionPane;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;

import application.editor.FlowCellEditor;
import models.controlFlowModel.CallEdgeAttribute;
import models.controlFlowModel.ObjectNode;
import models.controlFlowModel.ObjectNodeAttribute;

/*************************************************************
 * 
 */
public class ControlFlowDelegationCellEditor extends FlowCellEditor {
	private mxCell targetEdgeCell = null;

	/*************************************************************
	 *  [ *constructor ]
	/*************************************************************
	 * 
	 * @param graph
	 */
	public ControlFlowDelegationCellEditor(ControlFlowDelegationStage stage, mxGraphComponent graphComponent) {
		super(stage, graphComponent);
	}
	
	/*************************************************************
	 *  [ *public ]
	/*************************************************************
	 * 
	 */
	@Override
	public void startEditing(Object cellObj, EventObject eventObj) {
		if( editingCell != null) stopEditing(true);
		ControlFlowDelegationStage cfdStage = (ControlFlowDelegationStage)stage;
		
		switch(cfdStage.getCurState()) {
			case SELECTING_AN_EDGE:
				// Branching based on the edge click event.
				// | double clicked > Showing delegatable nodes.
				if( graphComponent.getGraph().getModel().isEdge(cellObj)) {
					// cache a target edge of cell;
					targetEdgeCell = (mxCell)cellObj;
					
					showDelegatableNodesBySelectedEdge(cellObj);
					cfdStage.setState(ControlFlowDelegationStageStatus.SHOWING_DELEGATABLE_NODES);
				}
				break;

			case SHOWING_DELEGATABLE_NODES:
				if( graphComponent.getGraph().getModel().isVertex(cellObj) ) {
					mxCell dstCell = null;
					if(cellObj instanceof mxCell) dstCell = (mxCell)cellObj;
					else throw new ClassCastException();

					// invocating delegation method
					CallEdgeAttribute callEdgeAttr = (CallEdgeAttribute)graphComponent.getGraph().getModel().getValue(targetEdgeCell);	
					if(callEdgeAttr == null) return;
					
					ObjectNode dstObjNode = ((ObjectNodeAttribute)dstCell.getValue()).getObjectNode();
					if(dstObjNode == null) throw new ClassCastException();
					
					if(!cfdStage.isExecutableDelegation(callEdgeAttr, dstObjNode)){
						JOptionPane.showMessageDialog(graphComponent, "It's impossible for the chose object to delegate.");
						return;
					}
					cfdStage.showDelegatedGraph(graphComponent.getGraph(), targetEdgeCell, (mxCell)cellObj);
					cfdStage.setState(ControlFlowDelegationStageStatus.SELECTING_AN_EDGE);
				}
				else {
					cfdStage.resetAllStyleOfCells();
					cfdStage.setState(ControlFlowDelegationStageStatus.SELECTING_AN_EDGE);
				}
				break;
			}
	}

	/*************************************************************
	 * 
	 */
	@Override
	public void stopEditing(boolean cancel) {	}

	/*************************************************************
	 * [ *private ]
	/*************************************************************
	 * view 
	/*************************************************************
	 * 
	 */
	private void showDelegatableNodesBySelectedEdge(Object cellObj) {
			CallEdgeAttribute callEdgeAttr = (CallEdgeAttribute)graphComponent.getGraph().getModel().getValue(cellObj);	
			if(callEdgeAttr == null) return;
			
			ControlFlowDelegationStage cfdStage = (ControlFlowDelegationStage)stage;	
			cfdStage.showDelegatableNodes(graphComponent.getGraph(), callEdgeAttr);
	}
}
