package application.editor;

import java.util.EventObject;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxICellEditor;

abstract public class FlowCellEditor implements mxICellEditor {
	
	protected Stage stage = null;
	protected mxGraphComponent graphComponent = null;

	protected Object editingCell = null;

	/*************************************************************
	 *  [ *constructor]
	/*************************************************************
	 * 
	 * @param stage
	 * @param graphComponent
	 */
	protected FlowCellEditor(Stage stage, mxGraphComponent graphComponent) {
		this.stage = stage;
		this.graphComponent = graphComponent;
	}
	
	/*************************************************************
	 *  [ *public ]
	/*************************************************************
	 * 
	 */
	public Object getEditingCell() { 
		return this.editingCell;
	}

	/*************************************************************
	 * 
	 */
	abstract public void startEditing(Object cellObj, EventObject eventObj);

	/*************************************************************
	 * 
	 */
	abstract public void stopEditing(boolean cancel);
}
