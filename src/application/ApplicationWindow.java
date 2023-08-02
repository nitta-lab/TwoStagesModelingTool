package application;

import javax.swing.JFrame;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.view.mxGraph;

import application.editor.Editor;
import application.views.NavigationWindow;
import application.views.controlFlowDelegation.FlowLayerWindow;

/**
 * Application main window
 * 
 * @author Nitta
 *
 */
public class ApplicationWindow extends JFrame {
	private static final long serialVersionUID = -8690140317781055614L;
	public static final String title = "Visual Modeling Tool";
	
	private Editor editor = null;
	private mxGraph graph = null;
	private mxGraphComponent graphComponent = null;

	private ApplicationMenuBar menuBar = null;
	private NavigationWindow navigationWindow = null;
	private FlowLayerWindow showFlowLayerWindow = null;

	public ApplicationWindow() {
		setTitle(title);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		this.graph = new mxGraph() {
			public boolean isPort(Object cell) {
				mxGeometry geo = getCellGeometry(cell);
				
				return (geo != null) ? geo.isRelative() : false;
			}
			
			public boolean isCellFoldable(Object cell, boolean collapse) {
				return false;
			}
		};
		
		this.graphComponent = new mxGraphComponent(graph);
		
		this.editor = new Editor(graphComponent);

		getContentPane().add(graphComponent);
		new mxRubberband(graphComponent);
		graph.setAllowDanglingEdges(false);
		graph.setCellsDisconnectable(true);
				
		menuBar = new ApplicationMenuBar(this);
		setJMenuBar(menuBar);
		setSize(870, 640);
		
		navigationWindow = new NavigationWindow(this, editor);
		navigationWindow.setVisible(true);
		
		showFlowLayerWindow = new FlowLayerWindow(this);
		showFlowLayerWindow.setVisible(false);
		
		editor.addStageChangeListener(navigationWindow);
		editor.addStageChangeListener(showFlowLayerWindow);
	}

	public mxGraph getGraph() {
		return graph;
	}

	public mxGraphComponent getGraphComponent() {
		return graphComponent;
	}

	public Editor getEditor() {
		return editor;
	}

	public void setEditor(Editor editor) {
		this.editor = editor;
	}

	public void showNavigationWindow() {
		navigationWindow.setVisible(true);
	}
	
	public void showSwitchLayerWindow() {
		showFlowLayerWindow.setVisible(true);
	}

}
