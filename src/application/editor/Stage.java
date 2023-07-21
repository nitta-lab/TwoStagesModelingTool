package application.editor;

import java.awt.event.MouseListener;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxICellEditor;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;

import models.dataFlowModel.DataTransferModel;

abstract public class Stage {
	protected DataTransferModel model = null;
	protected mxGraphComponent graphComponent = null;
	protected mxGraph graph = null;
	public static final int NODE_LAYER = 0;
	public static final int DATA_FLOW_LAYER = 0;
	public static final int PUSH_FLOW_LAYER = 1;
	public static final int PULL_FLOW_LAYER = 2;
	
	/*************************************************************
	 * [ *constructor]
 	/*************************************************************
 	 * 
 	 * @param graphComponent
   	 */
	public Stage(mxGraphComponent graphComponent) {
		this.graphComponent = graphComponent;
		this.graph = graphComponent.getGraph();
	}
	
	/*************************************************************
	 * 
	 * @return model
	 */
	public DataTransferModel getModel() {
		return model;
	}
	
	abstract public boolean canChangeFrom(Stage prevStage);
	abstract public void init(Stage prevStage);
	abstract public mxICellEditor createCellEditor(mxGraphComponent graphComponent);
	abstract public mxIEventListener createChangeEventListener(Editor editor);
	abstract public MouseListener createMouseEventListener(Editor editor);
	
	/*************************************************************
	 * [ *public ]
 	/*************************************************************
	 * 
	 */
	public void setEnabledForLayer(final int layerNo, final boolean isEnable) {
		mxCell rootCell = (mxCell) graph.getDefaultParent();
		if(rootCell== null) return;
		if(rootCell.getChildCount() <= 0) return;			
		
		graph.getModel().setVisible(rootCell.getChildAt(layerNo), isEnable);
		graph.refresh();
	}
	
	/*************************************************************
	 * Showing layers are specified number of layers. 
	 * @param you want to show numbers of layers.
	 */
	public void showOnlyLayer(final int... argsOfLayers) {
		mxCell rootCell = (mxCell) graph.getDefaultParent();
		if(rootCell== null) return;
		if(rootCell.getChildCount() <= 0) return;			
		
		for(int i = 0; i < rootCell.getChildCount(); i++) {
			graph.getModel().setVisible(rootCell.getChildAt(i), false);
		}
		
		for(int layerNo : argsOfLayers) {
			graph.getModel().setVisible(rootCell.getChildAt(layerNo), true);
		}
	}
}
