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
import generators.CodeGenerator;
import generators.CodeGeneratorFromControlFlowGraph;
import generators.CodeGeneratorFromDataFlowGraph;
import generators.DataTransferMethodAnalyzer;
import generators.JavaCodeGenerator;
import generators.JavaMethodBodyGenerator;
import generators.JavaSpecific;
import models.controlFlowModel.ControlFlowGraph;
import models.dataConstraintModel.IdentifierTemplate;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.ModelExtension;
import models.dataFlowModel.DataFlowGraph;

public class JavaPrototypeGenerateAction extends AbstractEditorAction {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3694103632055735068L;

	private String lastDir = null;

	public JavaPrototypeGenerateAction(Editor editor) {
		super("Generate Plain Java Prototype", editor);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		DataFlowGraph dataFlowgraph = editor.getDataFlowGraph();
		if (dataFlowgraph == null) {
			editor.changeStage(Editor.STAGE_PUSH_PULL_SELECTION);			
			dataFlowgraph = editor.getDataFlowGraph();
		}
		if (dataFlowgraph != null) {
			DataTransferModel model = editor.getModel();
			ModelExtension.extendModel(model);
			TypeInference.infer(model);
			DataTransferMethodAnalyzer.decideToStoreResourceStates(dataFlowgraph);
			String fileName = editor.getCurFileName();
			if (fileName == null) fileName = "Main";
			String mainTypeName = fileName.split("\\.")[0];
			boolean exist = false;
			for (IdentifierTemplate id: model.getIdentifierTemplates()) {
				String resourceName = id.getResourceName().substring(0, 1).toUpperCase() + id.getResourceName().substring(1);
				if (mainTypeName.equals(resourceName)) {
					exist = true;
				}
			}
			if (!exist) {
				CodeGenerator.setMainTypeName(mainTypeName);	// use model's file name as the main type's name.
			} else {
				CodeGenerator.resetMainTypeName();			// use the default main type's name.
			}
			ControlFlowGraph controlFlowGraph = editor.getControlFlowGraph();
			if (controlFlowGraph != null) {
				editor.setCodes(new CodeGeneratorFromControlFlowGraph().generateCode(model, controlFlowGraph, new JavaSpecific()));
			} else {
				editor.setCodes(new CodeGeneratorFromDataFlowGraph().generateCode(model, dataFlowgraph, new JavaSpecific()));
			}
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
