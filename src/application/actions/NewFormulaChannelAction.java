package application.actions;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import application.ApplicationWindow;
import application.editor.Editor;
import models.visualModel.FormulaChannel;

public class NewFormulaChannelAction extends AbstractEditorAction implements ActionListener {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 5345875219049178252L;
	
	public NewFormulaChannelAction(Editor editor) {
		super("FormulaChannel...", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JPanel panel = new JPanel();
		GridLayout layout = new GridLayout(2,2);
		panel.setLayout(layout);
		layout.setVgap(5);
		layout.setHgap(20);
		panel.add(new JLabel("Channel Name:"));
		JTextField channelText = new JTextField();
		panel.add(channelText);
		panel.add(new JLabel("Symbol:"));
		JTextField symbolText = new JTextField();
		panel.add(symbolText);

		int r = JOptionPane.showConfirmDialog(
			null,
			panel,
			"New Formula Channel",
			JOptionPane.OK_CANCEL_OPTION,
			JOptionPane.QUESTION_MESSAGE);
		
		String channelName = channelText.getText();
		String symbol = symbolText.getText();
		if(r == JOptionPane.OK_OPTION) {
			editor.addFormulaChannel(new FormulaChannel(channelName, editor.getModel().getSymbol(symbol)));
		}
	}
}
