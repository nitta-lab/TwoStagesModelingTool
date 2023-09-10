package application.editor;

import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mxgraph.layout.mxCircleLayout;
import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;

import application.editor.stages.ControlFlowDelegationStage;
import application.editor.stages.DataFlowModelingStage;
import application.editor.stages.PushPullSelectionStage;
import application.layouts.*;
import code.ast.CompilationUnit;
import models.EdgeAttribute;
import models.controlFlowModel.ControlFlowGraph;
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.DataFlowGraph;
import models.visualModel.FormulaChannel;
import parser.Parser;
import parser.exceptions.ExpectedAssignment;
import parser.exceptions.ExpectedChannel;
import parser.exceptions.ExpectedChannelName;
import parser.exceptions.ExpectedEquals;
import parser.exceptions.ExpectedFormulaChannel;
import parser.exceptions.ExpectedGeometry;
import parser.exceptions.ExpectedInOrOutOrRefKeyword;
import parser.exceptions.ExpectedIoChannel;
import parser.exceptions.ExpectedLeftCurlyBracket;
import parser.exceptions.ExpectedModel;
import parser.exceptions.ExpectedNode;
import parser.exceptions.ExpectedRHSExpression;
import parser.exceptions.ExpectedResource;
import parser.exceptions.ExpectedRightBracket;
import parser.exceptions.ExpectedStateTransition;
import parser.exceptions.WrongJsonExpression;
import parser.exceptions.WrongLHSExpression;
import parser.exceptions.WrongRHSExpression;
import parser.ParserDTRAM;

/**
 * Main editor for all stages
 * 
 * @author Nitta
 *
 */
public class Editor {
	public DataTransferModel model = null;
	public mxGraph graph = null;
	private mxGraphComponent  graphComponent = null;
	private mxIEventListener curChangeEventListener = null;
	private MouseListener curMouseEventListener = null;

	protected Stage curStage = null;
	protected List<Stage> stageQueue = null;
	private List<IStageChangeListener> stageChangeListeners = null;
		
	protected String curFileName = null;
	protected String curFilePath = null;
	protected ArrayList<CompilationUnit> codes = null;
	
	public static DataFlowModelingStage STAGE_DATA_FLOW_MODELING = null;
	public static PushPullSelectionStage STAGE_PUSH_PULL_SELECTION = null;
	public static ControlFlowDelegationStage STAGE_CONTROL_FLOW_DELEGATION = null;

	public Editor(mxGraphComponent graphComponent) {
		this.graphComponent = graphComponent;
		this.graph = graphComponent.getGraph();
		
		STAGE_DATA_FLOW_MODELING = new DataFlowModelingStage(graphComponent);
		STAGE_PUSH_PULL_SELECTION = new PushPullSelectionStage(graphComponent);
		STAGE_CONTROL_FLOW_DELEGATION = new ControlFlowDelegationStage(graphComponent);

		graphComponent.setCellEditor(STAGE_DATA_FLOW_MODELING.createCellEditor(graphComponent));
		
		stageQueue = new ArrayList<>();
		stageQueue.add(STAGE_DATA_FLOW_MODELING);
		stageQueue.add(STAGE_PUSH_PULL_SELECTION);
		stageQueue.add(STAGE_CONTROL_FLOW_DELEGATION);
		curStage = STAGE_DATA_FLOW_MODELING;
		
		stageChangeListeners = new ArrayList<>();
	}

	public mxGraph getGraph() {
		return graph;
	}

	public mxGraphComponent getGraphComponent() {
		return this.graphComponent;
	}
	
	public DataTransferModel getModel() {
		model = curStage.getModel();
		return model;
	}
	
	public Stage getCurStage() {
		return curStage;
	}
	
	public boolean canChange(Stage nextStage) {
		return nextStage.canChangeFrom(curStage);
	}
	
	public boolean nextStage() {
		int curStageNo = stageQueue.indexOf(curStage);
		if (curStageNo + 1 >= stageQueue.size()) return false;
		return changeStage(stageQueue.get(curStageNo + 1));
	}
	
	public boolean changeStage(Stage nextStage) {
		if (!nextStage.canChangeFrom(curStage)) return false;
		nextStage.init(curStage);
		graphComponent.setCellEditor(nextStage.createCellEditor(graphComponent));

		// add listeners
		// "curChangeEventListener" will be called when updating the mxGraph.
		if (curChangeEventListener != null) {
			graph.getModel().removeListener(curChangeEventListener);
		}
		curChangeEventListener = nextStage.createChangeEventListener(this);
		if (curChangeEventListener != null) {
			graph.getModel().addListener(mxEvent.CHANGE, curChangeEventListener);
		}

		// A handler of a mouse event.
		if(curMouseEventListener != null) {
			graphComponent.getGraphControl().removeMouseListener(curMouseEventListener);
		}
		curMouseEventListener = nextStage.createMouseEventListener(this);
		if(curMouseEventListener != null) {
			graphComponent.getGraphControl().addMouseListener(curMouseEventListener);
		}		
		curStage = nextStage;
		notifyStageChangeListeners();
		return true;
	}
	
	public void addStageChangeListener(IStageChangeListener stageChangeListener) {
		stageChangeListeners.add(stageChangeListener);
	}
	
	private void notifyStageChangeListeners() {
		for (IStageChangeListener l: stageChangeListeners) {
			l.stageChanged(curStage);
		}
	}

	public DataFlowGraph getDataFlowGraph() {
		if (curStage instanceof PushPullSelectionStage) {
			return ((PushPullSelectionStage) curStage).getDataFlowGraph();
		} else if (curStage instanceof ControlFlowDelegationStage) {
			return ((ControlFlowDelegationStage) curStage).getControlFlowGraph().getDataFlowGraph();
		}
		return null;
	}

	public ControlFlowGraph getControlFlowGraph() {
		if (curStage instanceof ControlFlowDelegationStage) {
			return ((ControlFlowDelegationStage) curStage).getControlFlowGraph();
		}
		return null;
	}
	
	public ArrayList<CompilationUnit> getCodes() {
		return codes;
	}

	public void setCodes(ArrayList<CompilationUnit> codes) {
		this.codes = codes;
	}

	public String getCurFileName() {
		return curFileName;
	}

	public String getCurFilePath() {
		return curFilePath;
	}

	public void setCurFilePath(String curFilePath) {
		this.curFilePath = curFilePath;
		this.curFileName = new File(curFilePath).getName();
	}

	public void clear() {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return;
		
		model = null;
		((DataFlowModelingStage) curStage).clear();
		
		curFilePath = null;
		curFileName = null;
		codes = null;
	}

	/**
	 * Open a given file, parse the file, construct a DataFlowModel and a mxGraph
	 * @param file given file
	 * @return a constructed DataFlowModel
	 */
	public DataTransferModel open(File file) {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return null;
		
		try {
			String extension ="";
			if(file != null && file.exists()) {
				// get file's name
				String name = file.getName();

				// get file's extension
				extension = name.substring(name.lastIndexOf("."));
			}
			if(extension.contains(".model")) {
				openModel(file);
			} else {
				// Parse the .dtram file.
				ParserDTRAM parserDTRAM = new ParserDTRAM(new BufferedReader(new FileReader(file)));
				try {
					model = parserDTRAM.doParseModel();
					if (curStage instanceof DataFlowModelingStage) {
						// Update the mxGraph.
						((DataFlowModelingStage) curStage).setModel(model);
					}
					// Restore the geometry.
					parserDTRAM.doParseGeometry(graph);
					
					// Change to the push/pull selection stage, analyze the data transfer model and construct a data-flow graph.
					changeStage(STAGE_PUSH_PULL_SELECTION);
					
					curFilePath = file.getAbsolutePath();
					curFileName = file.getName();
					return model;
				} catch (ExpectedChannel | ExpectedChannelName | ExpectedLeftCurlyBracket | ExpectedInOrOutOrRefKeyword
						| ExpectedStateTransition | ExpectedEquals | ExpectedRHSExpression | WrongLHSExpression
						| WrongRHSExpression | ExpectedRightBracket | ExpectedAssignment | ExpectedModel | ExpectedGeometry | ExpectedNode | ExpectedResource | ExpectedFormulaChannel | ExpectedIoChannel | WrongJsonExpression e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	public DataTransferModel openModel(File file) {
		// Force to change to data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return null;
		
		try {
			// Parse the .model file.
			Parser parser = new Parser(new BufferedReader(new FileReader(file)));
			try {	
				model = parser.doParse();
				if (curStage instanceof DataFlowModelingStage) {
					// Update the mxGraph.
					((DataFlowModelingStage) curStage).setModel(model);
				}
				// Set DAG layout.
				setDAGLayout();
				
				// Change to the push/pull selection stage, analyze the data transfer model and construct a data-flow graph.
				changeStage(STAGE_PUSH_PULL_SELECTION);
				
				curFilePath = file.getAbsolutePath();
				curFileName = file.getName();
				return model;
			} catch (ExpectedChannel | ExpectedChannelName | ExpectedLeftCurlyBracket | ExpectedInOrOutOrRefKeyword
					| ExpectedStateTransition | ExpectedEquals | ExpectedRHSExpression | WrongLHSExpression
					| WrongRHSExpression | ExpectedRightBracket | ExpectedAssignment | WrongJsonExpression e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	/*************************************************************
	 * [save]
	/*************************************************************
	 * 
	 */
	public void save() {
		if (curFilePath != null) {
			try {
				File file = new File(curFilePath);
				String extension = "";
				if(file != null && file.exists()) {
					// get a file's name
					String name = file.getName();

					// get a file's extension
					extension = name.substring(name.lastIndexOf("."));
				}
				if(extension.contains(".model")) {
					saveModel(file);
				} else {
					FileWriter filewriter = new FileWriter(file);		        
					filewriter.write(toOutputString());
					filewriter.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void saveModel(File file) {
		if (curFilePath != null) {
			try {
				FileWriter filewriter = new FileWriter(file);			     
				filewriter.write(model.getSourceText());
				filewriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**--------------------------------------------------------------------------------
	 * get writing texts "dtram" file  information is written.
	 * 
	 * @return formatted "dtram" info texts.
	 */
	protected String toOutputString() {
		String fileString = "";
		
		fileString += "model {\n";
		fileString += this.model.getSourceText();
		fileString += "}\n";

		fileString += "geometry {\n";

		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell nodeLayer = (mxCell) root.getChildAt(Stage.NODE_LAYER);
		mxCell dataFlowLayer = (mxCell) root.getChildAt(Stage.DATA_FLOW_LAYER);
		for (int i = 0; i < graph.getModel().getChildCount(nodeLayer); i++) {
			Object cell = graph.getModel().getChildAt(nodeLayer, i);
			if (graph.getModel().isVertex(cell)) {
				mxGraphView view = graph.getView();
				mxCellState state = view.getState(cell);
				int x = (int) state.getX();
				int y = (int) state.getY();
				int w = (int) state.getWidth();
				int h = (int) state.getHeight();

				for (ResourcePath res: model.getResourcePaths()){
					if(res instanceof ResourcePath && state.getLabel().equals(res.getResourceName()))
						fileString += "\tnode r " + state.getLabel() + ":" + x + "," + y + "," + w + "," + h + "\n";
				}

				for (Channel ioC: model.getIOChannels()) {
					if(ioC instanceof Channel && state.getLabel().equals(ioC.getChannelName())) {
						fileString += "\tnode ioc " + state.getLabel() + ":" + x + "," + y + "," + w + "," + h + "\n";
					}
				}
			}
		}
		for (int i = 0; i < graph.getModel().getChildCount(dataFlowLayer); i++) {
			Object cell = graph.getModel().getChildAt(dataFlowLayer, i);
			if (graph.getModel().isVertex(cell)) {
				mxGraphView view = graph.getView();
				mxCellState state = view.getState(cell);
				int x = (int) state.getX();
				int y = (int) state.getY();
				int w = (int) state.getWidth();
				int h = (int) state.getHeight();

				for(Channel ch: model.getChannels()) {
					if(ch instanceof FormulaChannel && state.getLabel().equals(ch.getChannelName())) {
						fileString += "\tnode fc " + state.getLabel() + ":" + x + "," + y + "," + w + "," + h+"\n";		
					} else if(ch instanceof Channel && state.getLabel().equals(ch.getChannelName())) {
						fileString +="\tnode c " + state.getLabel() + ":" + x + "," + y + "," + w + "," + h+"\n";
					}
				}
			}
		}
		fileString += "}\n";
	
		return fileString;
	}
	
	public void setDAGLayout() {
		mxCell root = (mxCell) graph.getDefaultParent();
		mxCell dataFlowLayer = (mxCell) root.getChildAt(Stage.DATA_FLOW_LAYER);
		graph.getModel().beginUpdate();
		try {
			DAGLayout ctl = new DAGLayout(graph);
			ctl.execute(dataFlowLayer);
//			for(int i = 0; i < root.getChildCount(); i++) {
//				ctl.execute(root.getChildAt(i));
//			}
		} finally {
			graph.getModel().endUpdate();
		}
	}

	public void setTreeLayout() {
		mxCell root = (mxCell) graph.getDefaultParent();
		graph.getModel().beginUpdate();
		try {
			mxCompactTreeLayout ctl = new mxCompactTreeLayout(graph);
			ctl.setLevelDistance(100);
			//		ctl.setHorizontal(false);
			ctl.setEdgeRouting(false);
			for(int i = 0; i < root.getChildCount(); i++) {
				ctl.execute(root.getChildAt(i));
			}
		} finally {
			graph.getModel().endUpdate();
		}
	}

	public void setCircleLayout() {
		mxCell root = (mxCell) graph.getDefaultParent();
		graph.getModel().beginUpdate();
		try {
			mxCircleLayout ctl = new mxCircleLayout(graph);
			for(int i = 0; i < root.getChildCount(); i++) {
				ctl.execute(root.getChildAt(i));
			}
		} finally {
			graph.getModel().endUpdate();
		}
	}

	public void addResourcePath(ResourcePath res) {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return;
		
		((DataFlowModelingStage) curStage).addResourcePath(res);
		model = ((DataFlowModelingStage) curStage).getModel();
	}

	public void addChannel(DataTransferChannel channel) {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return;
		
		((DataFlowModelingStage) curStage).addChannel(channel);
		model = ((DataFlowModelingStage) curStage).getModel();

	}

	public void addIOChannel(DataTransferChannel ioChannel) {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return;
		
		((DataFlowModelingStage) curStage).addIOChannel(ioChannel);
		model = ((DataFlowModelingStage) curStage).getModel();
	}

	public void addFormulaChannel(FormulaChannel formulaChannel) {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return;
		
		((DataFlowModelingStage) curStage).addFormulaChannel(formulaChannel);
		model = ((DataFlowModelingStage) curStage).getModel();
	}

	public boolean connectEdge(mxCell edge, mxCell src, mxCell dst) {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return false;
		
		boolean isConnected = ((DataFlowModelingStage) curStage).connectEdge(edge, src, dst);

		return isConnected;
	}

	public void delete() {
		// Force to change to the data-flow modeling stage.
		boolean stageChanged = changeStage(STAGE_DATA_FLOW_MODELING);
		if (!stageChanged) return;
		
		((DataFlowModelingStage) curStage).delete();
	}

	public static class SrcDstAttribute extends EdgeAttribute {
		private Object src;
		private Object dst;

		public SrcDstAttribute(Object src, Object dst) {
			this.src = src;
			this.dst = dst;
		}

		public Object getSrouce() {
			return src;
		}

		public Object getDestination() {
			return dst;
		}

		public String toString() {
			return "";
		}
	}
}
