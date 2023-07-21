package application.views;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JToggleButton;

import application.ApplicationWindow;
import application.editor.Editor;
import application.editor.IStageChangeListener;
import application.editor.Stage;
import application.editor.stages.ControlFlowDelegationStage;
import application.editor.stages.DataFlowModelingStage;
import application.editor.stages.PushPullSelectionStage;

public class NavigationWindow extends JDialog implements IStageChangeListener {
	private String title = "Navigation";
	private Editor editor;
	private JToggleButton dataFlowModelingButton;
	private JToggleButton pushPullSelectionButton;
	private JToggleButton controlFlowDelegationButton;
	private boolean forbidReentry = false;
	
	public NavigationWindow(ApplicationWindow owner, Editor editor) {
		super(owner);
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		this.editor = editor;
		Container panel = getContentPane();
		panel.setLayout(new java.awt.GridLayout(3, 1));
		dataFlowModelingButton = new JToggleButton("Data-Flow Modeling");
		pushPullSelectionButton = new JToggleButton("PUSH/PULL Selection");
		controlFlowDelegationButton = new JToggleButton("Control-Flow Delegation");
		dataFlowModelingButton.addActionListener(new DataFlowModelingButtonListener());
		pushPullSelectionButton.addActionListener(new PushPullSelectionButtonListener());
		controlFlowDelegationButton.addActionListener(new ControlFlowDelegationButtonListener());
		ButtonGroup group = new ButtonGroup();
		group.add(dataFlowModelingButton);
		group.add(pushPullSelectionButton);
		group.add(controlFlowDelegationButton);
		panel.add(dataFlowModelingButton);
		panel.add(pushPullSelectionButton);
		panel.add(controlFlowDelegationButton);
		controlFlowDelegationButton.setEnabled(false);
		pushPullSelectionButton.setEnabled(false);
		dataFlowModelingButton.setSelected(true);
		pack();
		setResizable(false);
	}

	@Override
	public void stageChanged(Stage newStage) {
		if (forbidReentry) return;
		if (newStage instanceof DataFlowModelingStage) {
			dataFlowModelingButton.setSelected(true);
			if (editor.canChange(Editor.STAGE_PUSH_PULL_SELECTION)) {
				pushPullSelectionButton.setEnabled(true);
			} else {
				pushPullSelectionButton.setEnabled(false);
			}
			if (editor.canChange(Editor.STAGE_CONTROL_FLOW_DELEGATION)) {
				controlFlowDelegationButton.setEnabled(true);
			} else {
				controlFlowDelegationButton.setEnabled(false);				
			}
		} else if (newStage instanceof PushPullSelectionStage) {
			pushPullSelectionButton.setSelected(true);
			if (editor.canChange(Editor.STAGE_DATA_FLOW_MODELING)) {
				dataFlowModelingButton.setEnabled(true);
			} else {
				dataFlowModelingButton.setEnabled(false);
			}
			if (editor.canChange(Editor.STAGE_CONTROL_FLOW_DELEGATION)) {
				controlFlowDelegationButton.setEnabled(true);
			} else {
				controlFlowDelegationButton.setEnabled(false);
			}			
		} else if (newStage instanceof ControlFlowDelegationStage) {
			controlFlowDelegationButton.setSelected(true);
			if (editor.canChange(Editor.STAGE_DATA_FLOW_MODELING)) {
				dataFlowModelingButton.setEnabled(true);
			} else {
				dataFlowModelingButton.setEnabled(false);
			}
			if (editor.canChange(Editor.STAGE_PUSH_PULL_SELECTION)) {
				pushPullSelectionButton.setEnabled(true);
			} else {
				pushPullSelectionButton.setEnabled(false);
			}			
		}
	}
	
	private class DataFlowModelingButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			forbidReentry = true;
			editor.changeStage(Editor.STAGE_DATA_FLOW_MODELING);
			forbidReentry = false;
			if (editor.canChange(Editor.STAGE_PUSH_PULL_SELECTION)) {
				pushPullSelectionButton.setEnabled(true);
			} else {
				pushPullSelectionButton.setEnabled(false);
			}
			if (editor.canChange(Editor.STAGE_CONTROL_FLOW_DELEGATION)) {
				controlFlowDelegationButton.setEnabled(true);
			} else {
				controlFlowDelegationButton.setEnabled(false);				
			}
		}
	}
	
	private class PushPullSelectionButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			forbidReentry = true;
			editor.changeStage(Editor.STAGE_PUSH_PULL_SELECTION);
			forbidReentry = false;
			if (editor.canChange(Editor.STAGE_DATA_FLOW_MODELING)) {
				dataFlowModelingButton.setEnabled(true);
			} else {
				dataFlowModelingButton.setEnabled(false);
			}
			if (editor.canChange(Editor.STAGE_CONTROL_FLOW_DELEGATION)) {
				controlFlowDelegationButton.setEnabled(true);
			} else {
				controlFlowDelegationButton.setEnabled(false);
			}
		}
	}
	
	private class ControlFlowDelegationButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			forbidReentry = true;
			editor.changeStage(Editor.STAGE_CONTROL_FLOW_DELEGATION);
			forbidReentry = false;
			if (editor.canChange(Editor.STAGE_DATA_FLOW_MODELING)) {
				dataFlowModelingButton.setEnabled(true);
			} else {
				dataFlowModelingButton.setEnabled(false);
			}
			if (editor.canChange(Editor.STAGE_PUSH_PULL_SELECTION)) {
				pushPullSelectionButton.setEnabled(true);
			} else {
				pushPullSelectionButton.setEnabled(false);
			}
		}
	}
}
