package application.actions;

import java.awt.event.ActionEvent;

import application.editor.Editor;

public class DeleteAction extends AbstractEditorAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4410145389391154784L;

	public DeleteAction(Editor editor) {
		super("Delete", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		editor.delete();
	}

}
