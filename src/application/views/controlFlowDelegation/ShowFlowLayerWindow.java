package application.views.controlFlowDelegation;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JDialog;

import application.ApplicationWindow;
import application.editor.Editor;
import application.editor.IStageChangeListener;
import application.editor.Stage;
import application.editor.stages.ControlFlowDelegationStage;

/*************************************************************
 * the window has a button group for swichting layers in the control-flow-modeling.
 */
public class ShowFlowLayerWindow extends JDialog implements IStageChangeListener {
	private String title = "ShowFlowLayerWindow";	
	private JCheckBox dataFlowCheckBox = null;
	private JCheckBox pushFlowCheckBox = null;
	private JCheckBox pullFlowCheckBox = null;
	
	private ControlFlowDelegationStage stage = null;
	
	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 * 
	 */
	public ShowFlowLayerWindow(final ApplicationWindow owner) {
		super(owner);
		
		setTitle(title);
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		stage = Editor.STAGE_CONTROL_FLOW_DELEGATION;
		
		// initialize buttons
		dataFlowCheckBox  = new JCheckBox("Data-Flow", false);
		pushFlowCheckBox = new JCheckBox("Push-Flow", true);
		pullFlowCheckBox   = new JCheckBox("Pull-Flow", true);

		// each add handler
		dataFlowCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stage.setEnabledForLayer(Stage.DATA_FLOW_LAYER, dataFlowCheckBox.isSelected());	
			}});
		
		pushFlowCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stage.setEnabledForLayer(Stage.PUSH_FLOW_LAYER, pushFlowCheckBox.isSelected());				
			}});

		pullFlowCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stage.setEnabledForLayer(Stage.PULL_FLOW_LAYER, pullFlowCheckBox.isSelected());		
			}});	
			
		dataFlowCheckBox.setEnabled(false);
		pushFlowCheckBox.setEnabled(false);
		pullFlowCheckBox.setEnabled(false);
	
		// initialize panel
		Container panel = getContentPane();
		panel.setLayout(new GridLayout(/*low*/3, /*col*/1));
		panel.add(dataFlowCheckBox);
		panel.add(pushFlowCheckBox);
		panel.add(pullFlowCheckBox);
		
		pack();
		setResizable(false);	
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * 
	 */
	@Override
	public void stageChanged(Stage newStage) {
		if((newStage instanceof ControlFlowDelegationStage)) {
			
			dataFlowCheckBox.setEnabled(true);
			pushFlowCheckBox.setEnabled(true);
			pullFlowCheckBox.setEnabled(true);
			
			newStage.setEnabledForLayer(Stage.PUSH_FLOW_LAYER, pushFlowCheckBox.isSelected());
			newStage.setEnabledForLayer(Stage.PULL_FLOW_LAYER, pullFlowCheckBox.isSelected());			
		}
		else {
			dataFlowCheckBox.setEnabled(false);
			pushFlowCheckBox.setEnabled(false);
			pullFlowCheckBox.setEnabled(false);
			
			newStage.setEnabledForLayer(Stage.PUSH_FLOW_LAYER, false);
			newStage.setEnabledForLayer(Stage.PULL_FLOW_LAYER, false);			
		}	
	}	
}	

