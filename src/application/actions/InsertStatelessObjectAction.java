package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;

import application.editor.stages.ControlFlowDelegationStage;
import application.editor.stages.ControlFlowDelegationStageStatus;
import models.controlFlowModel.ObjectNodeAttribute;

/*************************************************************
 * 
 */
public class InsertStatelessObjectAction extends AbstractPopupAction{
	
	/*************************************************************
	 *  [ *constructor ]
	/*************************************************************
	 */
	private ControlFlowDelegationStage stage = null;
	
	/*************************************************************
	 *  [ *constructor ]
	/*************************************************************
	 */
	public InsertStatelessObjectAction(final ControlFlowDelegationStage stage, final mxGraphComponent graphComponent ,final mxCell cell) {
		super("Insert Mediator", cell, graphComponent);
		this.stage = stage;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		insertObjectNode(targetCell);
		stage.setState( ControlFlowDelegationStageStatus.SELECTING_AN_EDGE);
	}

	/*************************************************************/
	private void insertObjectNode(final Object cellObj) {
		if(cellObj == null) return;
		
		mxCell edgeCell = null;
		if(cellObj instanceof mxCell) edgeCell = (mxCell)cellObj;
		else return;
				
		// Inputing name to the dialog.
		String objName = JOptionPane.showInputDialog("Object Name:");
		if(objName == null) return;

		if( objName.isEmpty()) {
			JOptionPane.showMessageDialog(graphComponent, "You must input a name. \nIt mustn't be empty.");
			return;
		}
		
		if( isDuplicatedName(objName) ) {
			JOptionPane.showMessageDialog(graphComponent, "The named object has already existed.");
			return;
		}
		
		stage.insertObjectNodeCellInControlFlowLayer(graphComponent.getGraph(), edgeCell, objName);
	}
	
	/*************************************************************
	 * 
	 */
	private boolean isDuplicatedName(final String name) {
		mxCell root = (mxCell)graphComponent.getGraph().getDefaultParent();
	
		for(int i = 0; i < root.getChildCount(); i++) {
			mxCell layerCell = (mxCell)root.getChildAt(i);
			for(int j = 0; j < layerCell.getChildCount(); j++) {
				mxCell cell = (mxCell)layerCell.getChildAt(j);
				
				ObjectNodeAttribute attr = null;
				if(cell.getValue() instanceof ObjectNodeAttribute)
					attr = (ObjectNodeAttribute)cell.getValue();
				else continue;
				
				if( !(attr.getObjectNode().getName().equals(name))) continue;
				return true;
			}
		}		
		return false;
	}


}
