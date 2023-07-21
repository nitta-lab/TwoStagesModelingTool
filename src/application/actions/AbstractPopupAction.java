package application.actions;

import javax.swing.AbstractAction;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;

public abstract class AbstractPopupAction extends AbstractAction {
	protected mxGraphComponent graphComponent = null;
	protected mxCell targetCell = null;
	
	public AbstractPopupAction(final String name, final mxCell targetCell, final mxGraphComponent graphComponent) {
		super(name);
		this.graphComponent = graphComponent;
		this.targetCell = targetCell;
	}
	
	public void updateTargetCell(final mxCell targetCell) {
		this.targetCell = targetCell;
	}
}
