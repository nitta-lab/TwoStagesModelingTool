package application.actions;

import java.awt.event.ActionEvent;

import application.editor.Editor;

public class DAGLayoutAction extends AbstractEditorAction {

	public DAGLayoutAction(Editor editor) {
		super("DAG Layout", editor);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		editor.setDAGLayout();
	}

}
