package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.mxgraph.swing.mxGraphComponent;

import application.editor.Editor;

public class ZoomOutAction extends AbstractViewerAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8657530769383486605L;

	public ZoomOutAction(mxGraphComponent graphComponent) {
		super("Zoom Out", graphComponent);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		graphComponent.zoomOut();
	}

}
