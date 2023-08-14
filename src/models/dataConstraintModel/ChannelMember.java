package models.dataConstraintModel;

import java.util.ArrayList;
import java.util.List;

public class ChannelMember {
	private ResourcePath resourcePath = null;
	private List<Selector> selectors = null;
	private StateTransition stateTransition = null;
	
	public ChannelMember(ResourcePath resourcePath) {
		this.resourcePath = resourcePath;
		selectors = new ArrayList<>();
		stateTransition = new StateTransition();
	}

	public ResourcePath getResource() {
		return resourcePath;
	}

	public void setResource(ResourcePath resourcePath) {
		this.resourcePath = resourcePath;
	}

	public List<Selector> getSelectors() {
		return selectors;
	}

	public void setSelectors(List<Selector> selectors) {
		this.selectors = selectors;
	}

	public ChannelMember addSelector(Selector selector) {
		selectors.add(selector);
		return this;
	}

	public StateTransition getStateTransition() {
		return stateTransition;
	}

	public void setStateTransition(StateTransition stateTransition) {
		this.stateTransition = stateTransition;
	}
	
	@Override
	public String toString() {
		if (stateTransition.getNextStateExpression() == null) {
			return resourcePath.getResourceName() + "("
					+ stateTransition.getCurStateExpression() + ","
					+ stateTransition.getMessageExpression() + ")";
		}
		return resourcePath.getResourceName() + "("
					+ stateTransition.getCurStateExpression() + ","
					+ stateTransition.getMessageExpression() + ")"
					+ " == " + stateTransition.getNextStateExpression();
	}
}
