package application.editor.stages;

import java.awt.Rectangle;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;

import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

import application.editor.FlowCellEditor;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;

/*************************************************************
 * 
 */
public class PushPullSelectionCellEditor extends FlowCellEditor {
	public int DEFAULT_MIN_WIDTH = 70;
	public int DEFAULT_MIN_HEIGHT = 30;
	public double DEFAULT_MINIMUM_EDITOR_SCALE = 1;

	protected double minimumEditorScale = DEFAULT_MINIMUM_EDITOR_SCALE;
	protected int minimumWidth = DEFAULT_MIN_WIDTH;
	protected int minimumHeight = DEFAULT_MIN_HEIGHT;

	private EventObject trigger;
	private JComboBox<String> comboBox;	

	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 * @param stage
	 * @param graphComponent
	 */
	public PushPullSelectionCellEditor(PushPullSelectionStage stage, mxGraphComponent graphComponent) {
		super(stage, graphComponent);
	}

	/*************************************************************
	 * 
	 * @param cellObj
	 * @param eventObj
	 */
	@Override
	public void startEditing(Object cellObj, EventObject eventObj) {
		if (editingCell != null) {
			stopEditing(true);
		}
		
		if (graphComponent.getGraph().getModel().isEdge(cellObj)) {
			mxCellState state = graphComponent.getGraph().getView().getState(cellObj);
			if (state != null && state.getLabel() != null && !state.getLabel().equals("")) {
				editingCell = cellObj;
				trigger = eventObj;
	
				double scale = Math.max(minimumEditorScale, graphComponent.getGraph().getView().getScale());
				Object value = graphComponent.getGraph().getModel().getValue(cellObj);
				if (value != null && value instanceof PushPullAttribute) {
					PushPullAttribute attr = (PushPullAttribute) value;
					comboBox = new JComboBox<>(attr.getOptionStrings());
					comboBox.setBorder(BorderFactory.createEmptyBorder());
					comboBox.setOpaque(false);
					comboBox.setBounds(getEditorBounds(state, scale));
					comboBox.setVisible(true);
					graphComponent.getGraphControl().add(comboBox, 0);
					comboBox.updateUI();
				}
			}
		}
	}

	/*************************************************************
	 * 
	 * @param cancel
	 */
	@Override
	public void stopEditing(boolean cancel) {
		if (editingCell != null) {
			comboBox.transferFocusUpCycle();
			Object cell = editingCell;
			editingCell = null;
			if (!cancel) {
				EventObject trig = trigger;
				trigger = null;
				Object value = graphComponent.getGraph().getModel().getValue(cell);
				if (value != null && value instanceof PushPullAttribute) {
					PushPullAttribute attr = (PushPullAttribute) value;
					List<PushPullValue> options = attr.getOptions();
					PushPullValue selected = null;
					for (PushPullValue option: options) {
						if (option.toString().equals(getCurrentValue())) {
							selected = option;
							break;
						}
					}
					if (selected != null) {
						options.remove(selected);
						options.add(0, selected);
					}
					graphComponent.labelChanged(cell, attr, trig);
				}
			} else {
				mxCellState state = graphComponent.getGraph().getView().getState(cell);
				graphComponent.redraw(state);
			}

			if (comboBox.getParent() != null) {
				comboBox.setVisible(false);
				comboBox.getParent().remove(comboBox);
			}

			graphComponent.requestFocusInWindow();
		}
	}

	public String getCurrentValue() {
		return (String) comboBox.getSelectedItem();
	}

	/**
	 * Returns the bounds to be used for the editor.
	 */
	public Rectangle getEditorBounds(mxCellState state, double scale) {
		mxIGraphModel model = state.getView().getGraph().getModel();
		Rectangle bounds = null;

		bounds = state.getLabelBounds().getRectangle();
		bounds.height += 10;

		// Applies the horizontal and vertical label positions
		if (model.isVertex(state.getCell())) {
			String horizontal = mxUtils.getString(state.getStyle(), mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_CENTER);

			if (horizontal.equals(mxConstants.ALIGN_LEFT)) {
				bounds.x -= state.getWidth();
			} else if (horizontal.equals(mxConstants.ALIGN_RIGHT)) {
				bounds.x += state.getWidth();
			}

			String vertical = mxUtils.getString(state.getStyle(),
					mxConstants.STYLE_VERTICAL_LABEL_POSITION,
					mxConstants.ALIGN_MIDDLE);

			if (vertical.equals(mxConstants.ALIGN_TOP)) {
				bounds.y -= state.getHeight();
			} else if (vertical.equals(mxConstants.ALIGN_BOTTOM)) {
				bounds.y += state.getHeight();
			}
		}

		bounds.setSize(
				(int) Math.max(bounds.getWidth(),
						Math.round(minimumWidth * scale)),
				(int) Math.max(bounds.getHeight(),
						Math.round(minimumHeight * scale)));

		return bounds;
	}
}
