package application.actions;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JFileChooser;

import algorithms.*;
import application.editor.Editor;
import code.ast.*;
import generators.DataTransferMethodAnalyzer;
import generators.JerseyCodeGenerator;
import generators.JerseyMethodBodyGenerator;
import models.algebra.Type;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.ModelExtension;
import models.dataFlowModel.DataFlowGraph;

public class JerseyPrototypeGenerateAction extends AbstractEditorAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1117065654665887436L;
	
	private String lastDir = null;

	public JerseyPrototypeGenerateAction(Editor editor) {
		super("Generate JAX-RS Prototype", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		DataFlowGraph graph = editor.getDataFlowGraph();
		if (graph == null) {
			editor.changeStage(Editor.STAGE_PUSH_PULL_SELECTION);
			graph = editor.getDataFlowGraph();			
		}
		if (graph != null) {
			DataTransferModel model = editor.getModel();
			ModelExtension.extendModel(model);
			TypeInference.infer(model);
			DataTransferMethodAnalyzer.decideToStoreResourceStates(graph);
			String fileName = editor.getCurFileName();
			if (fileName == null) fileName = "Main";
			String mainTypeName = fileName.split("\\.")[0];
			boolean exist = false;
			for (ResourcePath id: model.getResourcePaths()) {
				String resourceName = id.getResourceName().substring(0, 1).toUpperCase() + id.getResourceName().substring(1);
				if (mainTypeName.equals(resourceName)) {
					exist = true;
				}
			}
			if (!exist) {
				JerseyCodeGenerator.setMainTypeName(mainTypeName);	// use model's file name as the main type's name.
			} else {
				JerseyCodeGenerator.resetMainTypeName();			// use the default main type's name.
			}
			editor.setCodes(JerseyMethodBodyGenerator.doGenerate(graph, model, JerseyCodeGenerator.doGenerate(graph, model)));
			ModelExtension.recoverModel(model);
			for (CompilationUnit file : editor.getCodes()) {
				System.out.println(file);
			}
			
			String wd = (lastDir  != null) ? lastDir : System.getProperty("user.dir");
			JFileChooser fc = new JFileChooser(wd);
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int rc = fc.showSaveDialog(null);
			if (rc == JFileChooser.APPROVE_OPTION) {
				lastDir = fc.getSelectedFile().getPath();
				for (CompilationUnit cu : editor.getCodes()) {
					save(fc.getSelectedFile(), cu);
				}
			}
		}
	}

	private void save(File dir, CompilationUnit cu) {
		File javaFile = new File(dir.getPath(), cu.getFileName());
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(javaFile));
			writer.write(cu.toString());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
