package application.layouts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;

import models.controlFlowModel.ObjectNodeAttribute;
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.ResourceNodeAttribute;

public class DAGLayout extends mxGraphLayout {

	public DAGLayout(mxGraph graph) {
		super(graph);
	}

	@Override
	public void execute(Object parent) {
		mxIGraphModel model = graph.getModel();
		
		model.beginUpdate();
		try {
			List<List<mxCell>> map = new ArrayList<List<mxCell>>();
			List<String> moved = new ArrayList<>();
			
			for (int i = 0; i < model.getChildCount(parent); i++) {
				
				mxCell cell = (mxCell) model.getChildAt(parent, i);
				
				if (model.isVertex(cell)) {
					mxGraphView view = graph.getView();
					mxCellState state = view.getState(cell);
					
					if (!"ellipse".equals(state.getStyle().get("shape")) && (cell.getEdgeCount() == 1) && !"true".equals(state.getStyle().get("dashed"))) {
						List<mxCell> newline = new ArrayList<mxCell>();
						map.add(newline);
						lines(map, cell);
					}
				}
			}
			
			sort(map, 0, false);
			
			// layout
			int count;
			int skip = 0;
			mxGraphView view = graph.getView();
			for (int i = 0; i < map.size(); i++) {
				count = 0;
				for (int j = 0; j < map.get(i).size(); j++) {
					mxGeometry geom = (mxGeometry) map.get(i).get(j).getGeometry().clone();
					mxCellState state = view.getState(map.get(i).get(j));
					
					
					if (checkmoved(moved, map.get(i).get(j))) {
						if ("ellipse".equals(state.getStyle().get("shape"))){
							geom.setX(50 + j*200);
						} else {
							geom.setX(100 + j*200);
						}
						geom.setY(100 + (i-skip)*100);
						model.setGeometry(map.get(i).get(j), geom);
						moved.add(map.get(i).get(j).getId());
					} else if (geom.getX() < 100 + j*150) {
						if ("ellipse".equals(state.getStyle().get("shape"))){
							geom.setX(50 + j*200);
						} else {
							geom.setX(100 + j*200);
						}
						geom.setY(100 + (i-skip)*100);
						model.setGeometry(map.get(i).get(j), geom);
					} else {
						count++;
					}
				}
				if (count >= map.get(i).size())skip++;
			}
			
		} finally {
			model.endUpdate();
		}
	}
	
	
	public void lines(List<List<mxCell>> mapping, mxCell next) {
		mapping.get(mapping.size()-1).add(next);
		int tagcount = 0;
		mxCell edge;
		mxGraphView view = graph.getView();
		for (int i = 0; i < next.getEdgeCount(); i++) {
			edge = (mxCell) next.getEdgeAt(i);
			mxCellState state = view.getState(edge);
			if (next != (mxCell) edge.getTarget() && ((mxCell) edge.getTarget() != null) && !"true".equals(state.getStyle().get("dashed"))) {
				tagcount++;
				if (tagcount > 1) {
					List<mxCell> newline = new ArrayList<mxCell>(mapping.get(mapping.size()-1));
					while (newline.get(newline.size()-1).getId() != next.getId()) {
						newline.remove(newline.size()-1);
					}
					mapping.add(newline);
					lines(mapping, (mxCell) edge.getTarget());
					
				} else {
					lines(mapping, (mxCell) edge.getTarget());
				}
			}
		}
	}
	
	public boolean checkmoved(List<String> list, mxCell cell) {
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).equals(cell.getId()))return false;
		}
		return true;
	}
	
	public void sort(List<List<mxCell>> map, int n, boolean check) {
		int msize = -1;
		int mnum = -1;
		if (check) {
			for (int i = n; i < map.size(); i++) {
				if (map.get(i).size() > msize && (map.get(n-1).get(0).getId().equals(map.get(i).get(0).getId()))) {
					mnum = i;
				}
			}
		} else {
			for (int i = n; i < map.size(); i++) {
				if (map.get(i).size() > msize) {
					mnum = i;
				}
			}
		}
		if (mnum >= 0) {
			Collections.swap(map, n, mnum);
			sort(map, n+1, true);
		} else if(n < map.size()) {
			sort(map, n+1, false);
		}
	}
	
	
}