package application.views;

import java.awt.Component;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;

import application.actions.AbstractPopupAction;

/*************************************************************
 * 
 */
public abstract class PopupMenuBase {
	protected JPopupMenu popupMenu = null;
	protected mxGraphComponent graphComponent = null;

	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 */
	public PopupMenuBase(final mxGraphComponent graphComponent) {
		this.graphComponent = graphComponent;
		this.popupMenu = new JPopupMenu();
	}

	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * 
	 */
	public void show(int x, int y) {
		popupMenu.show(graphComponent, x, y);
	}

	/*************************************************************
	 * [ *protected ]
	/*************************************************************
	 * 
	 */
	protected void addMenuItem(JMenuItem menuItem) {
		popupMenu.add(menuItem);
	}

}
