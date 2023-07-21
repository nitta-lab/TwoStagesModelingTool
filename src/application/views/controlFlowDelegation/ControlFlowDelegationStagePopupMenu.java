package application.views.controlFlowDelegation;

import java.awt.Component;

import javax.swing.JMenuItem;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;

import application.actions.AbstractPopupAction;
import application.actions.ChangeCallOrderAction;
import application.actions.InsertStatelessObjectAction;
import application.editor.stages.ControlFlowDelegationStage;
import application.views.PopupMenuBase;

/*************************************************************
 * 
 */
public class ControlFlowDelegationStagePopupMenu extends PopupMenuBase {
	private ControlFlowDelegationStage stage = null;
	private mxCell selectedCell = null;

	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 */
	public ControlFlowDelegationStagePopupMenu(final ControlFlowDelegationStage stage, mxGraphComponent graphComponent) {
		super(graphComponent);
		this.stage = stage;

		addMenuItem(new JMenuItem(new InsertStatelessObjectAction(stage, graphComponent, selectedCell)));
		addMenuItem(new JMenuItem(new ChangeCallOrderAction(graphComponent, selectedCell)));
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * 
	 */
	@Override
	public void show(int x, int y) {

		boolean isEnable = (graphComponent.getCellAt(x, y) != null)
										? true
										: false;

		setEnableMenuItems(isEnable);
		super.show(x, y);

		
		if( graphComponent.getCellAt(x, y) instanceof mxCell ) {
			selectedCell =(mxCell) graphComponent.getCellAt(x, y);
		}
		else {
			selectedCell = null;
		}

		notifyCellCached(selectedCell);
	}

	/*************************************************************
	 * [ *private ]
	/*************************************************************
	 */
	private void notifyCellCached(final mxCell cell) {
		if(cell == null) return;

		for(Component component : popupMenu.getComponents()) {
			JMenuItem menuItem = null;
			if(component instanceof JMenuItem) menuItem = (JMenuItem)component;
			else return;

			AbstractPopupAction action  = null;
			if(menuItem.getAction() instanceof AbstractPopupAction) {
				action = (AbstractPopupAction)menuItem.getAction();
			}
			else return;

			action.updateTargetCell(cell);
		}
	}
	
	/*************************************************************
	 * 
	 */
	private void setEnableMenuItems(final boolean isEnable) {
		for(Component component : popupMenu.getComponents()) {
			component.setEnabled(isEnable);
		}
	}
}
