package application.actions;

import java.awt.event.ActionEvent;

import application.ApplicationWindow;
import application.editor.Editor;

public class NewModelAction extends AbstractSystemAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8484493203589724589L;

	public NewModelAction(ApplicationWindow frame) {
		super("Model", frame);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		frame.getEditor().clear();
		frame.setTitle(frame.title);		
	}

}
