package application.editor.stages;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.EventObject;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxICellEditor;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

import application.editor.Editor;
import application.editor.FlowCellEditor;
import models.algebra.Expression;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.visualModel.FormulaChannel;
import parser.Parser;
import parser.Parser.TokenStream;
import parser.exceptions.ExpectedColon;
import parser.exceptions.ExpectedRightBracket;
import parser.exceptions.WrongJsonExpression;

/*************************************************************
 * 
 */
public class DataFlowCellEditor  extends FlowCellEditor {	
	
	/*************************************************************
	 *  [ *constructor ]
	/*************************************************************
	 * 
	 * @param stage
	 * @param graphComponent
	 */
	public DataFlowCellEditor(DataFlowModelingStage stage, mxGraphComponent graphComponent) {
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

		if (!graphComponent.getGraph().getModel().isEdge(cellObj)) {
			DataTransferChannel ch = (DataTransferChannel) stage.getModel().getChannel((String) ((mxCell) cellObj).getValue());
			if (ch == null) {
				ch = (DataTransferChannel) stage.getModel().getIOChannel((String) ((mxCell) cellObj).getValue());
				if(ch == null) {
					//resource
					return;
				}
			}

			if (ch instanceof FormulaChannel) {

				JPanel panel = new JPanel();
				JLabel label1 = new JLabel("Formula: ");
				JLabel label2 = new JLabel("Source: ");
				GridBagLayout layout = new GridBagLayout();
				panel.setLayout(layout);
				GridBagConstraints gbc = new GridBagConstraints();

				gbc.gridx = 0;
				gbc.gridy = 0;
				layout.setConstraints(label1, gbc);
				panel.add(label1);

				gbc.gridx = 1;
				gbc.gridy = 0;
				JTextField formulaText = new JTextField(((FormulaChannel) ch).getFormula(),15);
				layout.setConstraints(formulaText, gbc);
				panel.add(formulaText);

				gbc.gridx = 0;
				gbc.gridy = 1;
				layout.setConstraints(label2, gbc);
				panel.add(label2);

				gbc.gridx = 1;
				gbc.gridy = 1;
				JTextArea textArea = new JTextArea(ch.getSourceText(),7,15);
				textArea.setEditable(false);
				layout.setConstraints(textArea, gbc);
				panel.add(textArea);

				int r = JOptionPane.showConfirmDialog(
						null,				// owner window
						panel,				// message
						"Edit Formula Channel",			// window's title
						JOptionPane.OK_CANCEL_OPTION,	// option (button types)
						JOptionPane.QUESTION_MESSAGE);	// message type (icon types)
				if (r == JOptionPane.OK_OPTION) {
					TokenStream stream = new Parser.TokenStream();
					Parser parser = new Parser(stream);

					String formula = formulaText.getText();
					stream.addLine(formula.split(Parser.EQUALS)[1]);

					
					try {
						Expression exp = parser.parseTerm(stream, stage.getModel());
						((FormulaChannel) ch).setFormula(formula);
						((FormulaChannel) ch).setFormulaTerm(exp);
					} catch (ExpectedRightBracket | WrongJsonExpression | ExpectedColon e) {
						e.printStackTrace();
					}
				}
			} else {
				JPanel panel = new JPanel();
				JTextArea textArea = new JTextArea(ch.getSourceText(), 10, 20);
				panel.add(textArea);
				//			JEditorPane panel = new JEditorPane("text/plain", ch.toString());
				//			panel.setEditable(true);
				int ret = JOptionPane.showConfirmDialog(null, panel, "Channel Code", JOptionPane.OK_CANCEL_OPTION);
				if (ret == JOptionPane.OK_OPTION) {
						((DataFlowModelingStage)stage).setChannelCode(ch, textArea.getText());
				}
			}
			return;
		}
	}

	/*************************************************************
	 * 
	 * @param cancel
	 */
	@Override
	public void stopEditing(boolean cancel) {
	}
}
