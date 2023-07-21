package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxResources;

import application.editor.Editor;

public class ZoomInAction extends AbstractViewerAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 6758532166946195926L;

	public ZoomInAction(mxGraphComponent graphComponent) {
		super("Zoom In", graphComponent);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		graphComponent.zoomIn();
	}

}
