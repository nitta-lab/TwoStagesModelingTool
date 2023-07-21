package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import application.editor.Editor;
import models.dataConstraintModel.IdentifierTemplate;

public class NewResourceAction extends AbstractEditorAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4439207504700741286L;

	public NewResourceAction(Editor editor) {
		super("Resource...", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String resName = JOptionPane.showInputDialog("Resourece Name:");
		if (resName == null) return;
		editor.addIdentifierTemplate(new IdentifierTemplate(resName, 0));
	}

}
