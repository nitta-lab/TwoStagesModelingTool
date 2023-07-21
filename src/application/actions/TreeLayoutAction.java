package application.actions;

import java.awt.event.ActionEvent;

import application.editor.Editor;

public class TreeLayoutAction extends AbstractEditorAction {

	public TreeLayoutAction(Editor editor) {
		super("Tree Layout", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		editor.setTreeLayout();
	}

}
