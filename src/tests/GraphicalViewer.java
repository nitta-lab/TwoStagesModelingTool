package tests;


import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.CellRendererPane;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxImageCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.shape.mxITextShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.swing.view.mxCellEditor;
import com.mxgraph.swing.view.mxICellEditor;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;

public class GraphicalViewer extends JFrame
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -844106998814982739L;
	
	final int PORT_DIAMETER = 8;
	final int PORT_RADIUS = PORT_DIAMETER / 2;

	public GraphicalViewer()
	{
		super("Graphical Viewer");

		// Demonstrates the use of a Swing component for rendering vertices.
		// Note: Use the heavyweight feature to allow for event handling in
		// the Swing component that is used for rendering the vertex.

		mxGraph graph = new mxGraph() {
			public boolean isPort(Object cell) {
				mxGeometry geo = getCellGeometry(cell);
				
				return (geo != null) ? geo.isRelative() : false;
			}
			
			public boolean isCellFoldable(Object cell, boolean collapse) {
				return false;
			}
		};

		Object parent = graph.getDefaultParent();

		graph.getModel().beginUpdate();
		try {
			mxGeometry geo1 = new mxGeometry(0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo1.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo1.setRelative(true);
			
			mxGeometry geo2 = new mxGeometry(1.0, 0.5, PORT_DIAMETER, PORT_DIAMETER);
			geo2.setOffset(new mxPoint(-PORT_RADIUS, -PORT_RADIUS));
			geo2.setRelative(true);
			
			Object c1 = graph.insertVertex(parent, null, "c1", 150, 20, 30, 30);
			mxCell c1_in = new mxCell(null, geo1, "shape=ellipse;perimter=ellipsePerimeter");
			c1_in.setVertex(true);			
			graph.addCell(c1_in, c1);
			mxCell c1_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
			c1_out.setVertex(true);
			graph.addCell(c1_out, c1);
			
			Object c2 = graph.insertVertex(parent, null, "c2", 150, 100, 30, 30);
			mxCell c2_in = new mxCell(null, geo1, "shape=ellipse;perimter=ellipsePerimeter");
			c2_in.setVertex(true);			
			graph.addCell(c2_in, c2);
			mxCell c2_out = new mxCell(null, geo2, "shape=ellipse;perimter=ellipsePerimeter");
			c2_out.setVertex(true);
			graph.addCell(c2_out, c2);
			
			Object payment = graph.insertVertex(parent, null, "payment", 20, 20, 80, 30, "shape=ellipse;perimeter=ellipsePerimeter");
			Object points = graph.insertVertex(parent, null, "points", 240, 20, 80, 30, "shape=ellipse;perimeter=ellipsePerimeter");
			Object history = graph.insertVertex(parent, null, "history", 240, 150, 80, 30, "shape=ellipse;perimeter=ellipsePerimeter");
			
			graph.insertEdge(parent, null, "PUSH/PULL", payment, c1_in);
			graph.insertEdge(parent, null, "", c1_out, points);
			
			graph.insertEdge(parent, null, "PUSH/PULL", payment, c2_in);
			graph.insertEdge(parent, null, "", c2_out, history);
		} finally {
			graph.getModel().endUpdate();
		}

		mxGraphComponent graphComponent = new mxGraphComponent(graph) {			
			protected mxICellEditor createCellEditor() {
				final mxGraphComponent graphComponent = this;
				return new mxICellEditor() {
					/**
					 * 
					 */
					public int DEFAULT_MIN_WIDTH = 70;
					public int DEFAULT_MIN_HEIGHT = 30;
					public double DEFAULT_MINIMUM_EDITOR_SCALE = 1;
					
					protected double minimumEditorScale = DEFAULT_MINIMUM_EDITOR_SCALE;
					protected int minimumWidth = DEFAULT_MIN_WIDTH;
					protected int minimumHeight = DEFAULT_MIN_HEIGHT;
					
					private Object editingCell;
					private EventObject trigger;
					private JComboBox<String> comboBox;
					
					@Override
					public Object getEditingCell() {
						return editingCell;
					}

					@Override
					public void startEditing(Object cell, EventObject evt) {
						if (editingCell != null) {
							stopEditing(true);
						}

						mxCellState state = graphComponent.getGraph().getView().getState(cell);
						if (state != null && state.getLabel() != null && !state.getLabel().equals("")) {
							editingCell = cell;
							trigger = evt;
							
							double scale = Math.max(minimumEditorScale, graphComponent.getGraph().getView().getScale());
							if (comboBox == null) {
								comboBox = new JComboBox<>(new String[]{"PUSH", "PULL"});
								comboBox.setBorder(BorderFactory.createEmptyBorder());
								comboBox.setOpaque(false);
							}
							comboBox.setBounds(getEditorBounds(state, scale));
							comboBox.setVisible(true);
							graphComponent.getGraphControl().add(comboBox, 0);
							comboBox.updateUI();
						}
					}

					@Override
					public void stopEditing(boolean cancel) {
						if (editingCell != null) {
							comboBox.transferFocusUpCycle();
							Object cell = editingCell;
							editingCell = null;
							if (!cancel) {
								EventObject trig = trigger;
								trigger = null;
								graphComponent.labelChanged(cell, getCurrentValue(), trig);
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
				};
			}
//			
//			public Component[] createComponents(mxCellState state) {
//				if (getGraph().getModel().isEdge(state.getCell()))
//				{
//					return new Component[] { new JComboBox(new String[]{"PUSH", "PULL"}) };
//				}
//
//				return null;
//			}
		};
		
		graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
		
			public void mouseReleased(MouseEvent e) {
				Object cell = graphComponent.getCellAt(e.getX(), e.getY());
				
				if (cell != null) {
					System.out.println("cell="+graph.getLabel(cell));
				}
			}
		});
		getContentPane().add(graphComponent);
	}

	public static void main(String[] args) {
		GraphicalViewer frame = new GraphicalViewer();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(400, 320);
		frame.setVisible(true);
	}

}
