package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.mxgraph.swing.mxGraphComponent;

public abstract class AbstractViewerAction extends AbstractAction {

	protected mxGraphComponent graphComponent = null;

	public AbstractViewerAction(String name, mxGraphComponent graphComponent) {
		super(name);
		this.graphComponent = graphComponent;
	}
}
