package models.controlFlowModel;

import models.Edge;
import models.dataFlowModel.PushPullValue;

public class CallEdge extends Edge {
	private PushPullValue selectedOption = PushPullValue.PUSHorPULL;
	
	public CallEdge(ObjectNode src, ObjectNode dst, PushPullValue selectedOption) {
		super(src, dst);
		this.selectedOption = selectedOption; 
	}
	
	public PushPullValue getSelectedOption() {
		return this.selectedOption;
	}
}
