package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import application.editor.Editor;
import models.dataFlowModel.DataTransferChannel;

public class NewIOChannelAction extends AbstractEditorAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1657072017390171313L;

	public NewIOChannelAction(Editor editor) {
		super("I/O Channel", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String channelName = JOptionPane.showInputDialog("I/O Channel Name:");
		if (channelName == null) return;
		editor.addIOChannel(new DataTransferChannel(channelName));
	}

}
