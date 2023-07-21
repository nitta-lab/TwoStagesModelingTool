package models.dataConstraintModel;

import java.util.ArrayList;
import java.util.List;

public class ChannelMember {
	private IdentifierTemplate identifierTemplate = null;
	private List<Selector> selectors = null;
	private StateTransition stateTransition = null;
	
	public ChannelMember(IdentifierTemplate identifierTemplate) {
		this.identifierTemplate = identifierTemplate;
		selectors = new ArrayList<>();
		stateTransition = new StateTransition();
	}

	public IdentifierTemplate getIdentifierTemplate() {
		return identifierTemplate;
	}

	public void setIdentifierTemplate(IdentifierTemplate identifierTemplate) {
		this.identifierTemplate = identifierTemplate;
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
			return identifierTemplate.getResourceName() + "("
					+ stateTransition.getCurStateExpression() + ","
					+ stateTransition.getMessageExpression() + ")";
		}
		return identifierTemplate.getResourceName() + "("
					+ stateTransition.getCurStateExpression() + ","
					+ stateTransition.getMessageExpression() + ")"
					+ " == " + stateTransition.getNextStateExpression();
	}
}
