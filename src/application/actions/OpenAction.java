package application.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import application.ApplicationWindow;
import application.editor.Editor;

public class OpenAction extends AbstractSystemAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8290761032629599683L;
	
	private String lastDir = null;
	
	public OpenAction(ApplicationWindow frame) {
		super("Open...", frame);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Editor editor = frame.getEditor();
		if (editor != null) {
			String wd = (lastDir  != null) ? lastDir : System.getProperty("user.dir");

			JFileChooser fc = new JFileChooser(wd);

			FileFilter model = new FileNameExtensionFilter("model","model");
			FileFilter dtram = new FileNameExtensionFilter("dtram", "dtram");
			
			// Adds file filter for supported file format
			FileFilter defaultFilter = new FileFilter() {

				public boolean accept(File file) {
					String lcase = file.getName().toLowerCase();
					return lcase.endsWith(".model");
				}

				@Override
				public String getDescription() {
					return null;
				}
			};
			
			fc.addChoosableFileFilter(defaultFilter);
			fc.addChoosableFileFilter(model);
			fc.addChoosableFileFilter(dtram);
			int rc = fc.showDialog(null, "Open Model File");
			if (rc == JFileChooser.APPROVE_OPTION) {
				lastDir = fc.getSelectedFile().getParent();
				editor.open(fc.getSelectedFile());
				frame.setTitle(frame.title + " - " + fc.getSelectedFile().getAbsolutePath());
			}
		}
	}

}
