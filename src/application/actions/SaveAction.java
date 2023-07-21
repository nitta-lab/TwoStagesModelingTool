package application.actions;

import java.awt.event.ActionEvent;

import application.ApplicationWindow;
import application.editor.Editor;

public class SaveAction extends AbstractSystemAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5660460585305281982L;

	public SaveAction(ApplicationWindow frame) {
		super("Save", frame);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Editor editor = frame.getEditor();
		if (editor != null && editor.getCurFileName() != null) {
			editor.save();
		}
	}

}
