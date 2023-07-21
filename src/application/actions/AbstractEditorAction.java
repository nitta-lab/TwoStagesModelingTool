package application.actions;

import javax.swing.AbstractAction;
import javax.swing.Icon;

import com.mxgraph.view.mxGraph;

import application.editor.Editor;

public abstract class AbstractEditorAction extends AbstractAction {

	protected Editor editor;

	public AbstractEditorAction(String name, Editor editor) {
		super(name);
		this.editor = editor;
	}

	public void setEditor(Editor editor) {
		this.editor = editor;
	}

}