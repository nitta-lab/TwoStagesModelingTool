package application.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import application.ApplicationWindow;
import application.editor.Editor;

public class SaveAsAction extends AbstractSystemAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2599502783032684084L;

	private String lastDir = null;

	public SaveAsAction(ApplicationWindow frame) {
		super("Save As...", frame);
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
			int rc = fc.showDialog(null, "Save Model File");
			
			// choose a file extension from a dialog.
			if (rc == JFileChooser.APPROVE_OPTION) {

				// if extension filter is filled, then attaching extension by choosing filter.
				// but if it's not filled, then using default extension name. 				
				String extension = "";				
				if(fc.getFileFilter() instanceof FileNameExtensionFilter) {
					FileNameExtensionFilter selectedFilter = (FileNameExtensionFilter)fc.getFileFilter();
					extension = "." + selectedFilter.getExtensions()[0].toString();
				}
				
				lastDir = fc.getSelectedFile().getParent();
				
				String fileName = fc.getSelectedFile().getAbsolutePath() + extension;
				editor.setCurFilePath(fileName);	
				
				// overwriting file
				editor.save();
				frame.setTitle(ApplicationWindow.title + " - " + fc.getSelectedFile().getAbsolutePath());
			}
		}
	}

}
