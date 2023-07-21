package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import application.editor.Editor;
import models.dataFlowModel.DataTransferChannelGenerator;

public class NewChannelAction extends AbstractEditorAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5979007029473101802L;
	
	public NewChannelAction(Editor editor) {
		super("Channel...", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String channelName = JOptionPane.showInputDialog("Channel Name:");
		if (channelName == null) return;
		editor.addChannelGenerator(new DataTransferChannelGenerator(channelName));
	}

}
