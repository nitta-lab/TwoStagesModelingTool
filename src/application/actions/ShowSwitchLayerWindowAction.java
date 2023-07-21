package application.actions;

import java.awt.event.ActionEvent;

import application.ApplicationWindow;

public class ShowSwitchLayerWindowAction extends AbstractSystemAction {
	public ShowSwitchLayerWindowAction(ApplicationWindow frame) {
		super("Show Flow", frame);
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		frame.showSwitchLayerWindow();
	}
}
