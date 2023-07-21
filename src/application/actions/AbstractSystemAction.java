package application.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import application.ApplicationWindow;
import application.editor.Editor;

public abstract class AbstractSystemAction extends AbstractAction {

	protected ApplicationWindow frame;

	public AbstractSystemAction(String name, ApplicationWindow frame) {
		super(name);
		this.frame = frame;
	}

	public void setFrame(ApplicationWindow frame) {
		this.frame = frame;
	}

}
