package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;

import models.controlFlowModel.CallEdgeAttribute;

/*************************************************************
 *
 */
public class ChangeCallOrderAction extends AbstractPopupAction {
	
	/*************************************************************
	 *  [ *constructor ]
	/*************************************************************
	 */
	public ChangeCallOrderAction(final mxGraphComponent graphComponent ,final mxCell cell) {
		super("Change Call Order", cell, graphComponent);
	}

	/*************************************************************
	 *  [ *public ]
	/*************************************************************
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(targetCell == null) return;
		changeCallOrderOfCallEdge(targetCell);
	}
	
	/*************************************************************
	 * [ *private ]
	/*************************************************************
	 */
	private void changeCallOrderOfCallEdge(Object cellObj) {		
		String input = "";
		int inputOrder = 0;

		CallEdgeAttribute callEdgeAttr = (CallEdgeAttribute)graphComponent.getGraph().getModel().getValue(cellObj);	
		if(callEdgeAttr == null) return;

		input = JOptionPane.showInputDialog("Call order");
		if( input == null) return;

		if( !isNumeric(input) ) {
			JOptionPane.showMessageDialog(graphComponent, "Input value must type of number.");
			return;
		}

		inputOrder = Integer.parseInt(input);

		final int endOfOrderOfSrc = callEdgeAttr.getSourceObjectNode().getOutdegree();
		if(inputOrder <= 0	|| endOfOrderOfSrc < inputOrder) {
			JOptionPane.showMessageDialog(graphComponent, "Input order must be between 1 and " + endOfOrderOfSrc + ".");
			return;
		}

		int curOrder = callEdgeAttr.getSourceObjectNode().getOutEdgeCallOrder(callEdgeAttr.getCallEdge());
		callEdgeAttr.getSourceObjectNode().sortOutEdgesByCallOrder(curOrder,  inputOrder);					

		graphComponent.refresh();
	}

	/*************************************************************
	 * 
	 */
	private boolean isNumeric(final String str) {
		if(str == null) return false;
		return str.matches("[0-9.]+");
	}
}