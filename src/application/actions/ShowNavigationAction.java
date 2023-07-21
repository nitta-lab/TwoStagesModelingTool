package application.actions;

import java.awt.event.ActionEvent;

import application.ApplicationWindow;

public class ShowNavigationAction extends AbstractSystemAction {

	public ShowNavigationAction(ApplicationWindow frame) {
		super("Show Navigation", frame);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		frame.showNavigationWindow();
	}

}
