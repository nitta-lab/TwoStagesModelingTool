package application;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import application.actions.CircleLayoutAction;
import application.actions.DAGLayoutAction;
import application.actions.DeleteAction;
import application.actions.ExitAction;
import application.actions.JavaPrototypeGenerateAction;
import application.actions.JerseyPrototypeGenerateAction;
import application.actions.NewChannelAction;
import application.actions.NewFormulaChannelAction;
import application.actions.NewIOChannelAction;
import application.actions.NewModelAction;
import application.actions.NewResourceAction;
import application.actions.OpenAction;
import application.actions.SaveAction;
import application.actions.SaveAsAction;
import application.actions.ShowNavigationAction;
import application.actions.ShowSwitchLayerWindowAction;
import application.actions.TreeLayoutAction;
import application.actions.ZoomInAction;
import application.actions.ZoomOutAction;
import application.editor.Editor;

public class ApplicationMenuBar extends JMenuBar {
	private static final long serialVersionUID = 4811536194182272888L;	
	
	private Editor editor = null;

	private ApplicationWindow applicationWindow = null;
	
	private NewResourceAction newResourceAction = null;
	private NewChannelAction newChannelAction = null;
	private NewIOChannelAction newIOChannelAction = null;
	private NewFormulaChannelAction newFormulaChannelAction = null;
	private DeleteAction deleteAction = null;
	private JavaPrototypeGenerateAction javaPrototypeGenerateAction = null;
	private JerseyPrototypeGenerateAction jerseyPrototypeGenerateAction = null;
	private DAGLayoutAction dagLayoutAction = null;
	private TreeLayoutAction treeLayoutAction = null;
	private CircleLayoutAction circleLayoutAction = null;
		
	public ApplicationMenuBar(ApplicationWindow applicationWindow) {
		this.applicationWindow = applicationWindow;
		this.editor = editor;
		JMenu newMenu = new JMenu("New");
		
		newMenu.add(new NewModelAction(applicationWindow));

		newMenu.add(newResourceAction = new NewResourceAction(applicationWindow.getEditor()));
		newMenu.add(newChannelAction = new NewChannelAction(applicationWindow.getEditor()));
		newMenu.add(newIOChannelAction = new NewIOChannelAction(applicationWindow.getEditor()));
		newMenu.add(newFormulaChannelAction = new NewFormulaChannelAction(applicationWindow.getEditor()));

		JMenu menu = null;
		menu = add(new JMenu("File"));
		menu.add(newMenu);
		menu.add(new OpenAction(applicationWindow));
		menu.addSeparator();
		menu.add(new SaveAction(applicationWindow));
		menu.add(new SaveAsAction(applicationWindow));
		menu.addSeparator();
		menu.add(new ExitAction());

		menu = add(new JMenu("Edit"));
		menu.add(deleteAction = new DeleteAction(applicationWindow.getEditor()));


		menu = add(new JMenu("Layout"));
		menu.add(dagLayoutAction  = new DAGLayoutAction(applicationWindow.getEditor()));
		menu.add(treeLayoutAction  = new TreeLayoutAction(applicationWindow.getEditor()));
		menu.add(circleLayoutAction   = new CircleLayoutAction(applicationWindow.getEditor()));

		menu = add(new JMenu("View"));
		menu.add(new ZoomInAction(applicationWindow.getGraphComponent()));
		menu.add(new ZoomOutAction(applicationWindow.getGraphComponent()));

		menu = add(new JMenu("Generate"));
		menu.add(javaPrototypeGenerateAction = new JavaPrototypeGenerateAction(applicationWindow.getEditor()));
		menu.add(jerseyPrototypeGenerateAction = new JerseyPrototypeGenerateAction(applicationWindow.getEditor()));
		
		menu = add(new JMenu("Window"));
		menu.add(new ShowNavigationAction(applicationWindow));
		menu.add(new ShowSwitchLayerWindowAction(applicationWindow));
	}

	public Editor getEditor() {
		return applicationWindow.getEditor();
	}

	public void setEditor(Editor editor) {
		newResourceAction.setEditor(editor);
		newChannelAction.setEditor(editor);
		newIOChannelAction.setEditor(editor);
		deleteAction.setEditor(editor);
		javaPrototypeGenerateAction.setEditor(editor);
		jerseyPrototypeGenerateAction.setEditor(editor);
		treeLayoutAction.setEditor(editor);
		circleLayoutAction.setEditor(editor);
	}
}
