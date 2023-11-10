package models.dataFlowModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import models.algebra.Expression;
import models.algebra.InvalidMessage;
import models.algebra.Parameter;
import models.algebra.ParameterizedIdentifierIsFutureWork;
import models.algebra.Position;
import models.algebra.Term;
import models.algebra.UnificationFailed;
import models.algebra.ValueUndefined;
import models.algebra.Variable;
import models.dataConstraintModel.*;

public class DataTransferChannel extends Channel {
	protected Set<ChannelMember> inputChannelMembers = null;
	protected Set<ChannelMember> outputChannelMembers = null;
	protected Set<ChannelMember> referenceChannelMembers = null;
	
	public DataTransferChannel(String channelName) {
		super(channelName);
		inputChannelMembers = new HashSet<>();
		outputChannelMembers = new HashSet<>();
		referenceChannelMembers = new HashSet<>();
	}
	
	public Set<ChannelMember> getInputChannelMembers() {
		return inputChannelMembers;
	}
	
	public void setInputChannelMembers(Set<ChannelMember> inputChannelMembers) {
		this.inputChannelMembers = inputChannelMembers;
	}
	
	private void addInputChannelMember(ChannelMember inputChannelMember) {
		inputChannelMembers.add(inputChannelMember);
	}
	
	public Set<ChannelMember> getOutputChannelMembers() {
		return outputChannelMembers;
	}
	
	public void setOutputChannelMembers(Set<ChannelMember> outputChannelMembers) {
		this.outputChannelMembers = outputChannelMembers;
	}
	
	private void addOutputChannelMember(ChannelMember outputChannelMember) {
		outputChannelMembers.add(outputChannelMember);
	}
	
	public Set<ChannelMember> getReferenceChannelMembers() {
		return referenceChannelMembers;
	}
	
	public void setReferenceChannelMembers(Set<ChannelMember> referenceChannelMembers) {
		this.referenceChannelMembers = referenceChannelMembers;
	}
	
	private void addReferenceChannelMember(ChannelMember referenceChannelMember) {
		referenceChannelMembers.add(referenceChannelMember);
	}
		
	public void addChannelMemberAsInput(ChannelMember groupDependentResource) {
		addChannelMember(groupDependentResource);
		addInputChannelMember(groupDependentResource);
	}
	
	public void addChannelMemberAsOutput(ChannelMember groupDependentResource) {
		addChannelMember(groupDependentResource);
		addOutputChannelMember(groupDependentResource);
	}
	
	public void addChannelMemberAsReference(ChannelMember groupDependentResource) {
		addChannelMember(groupDependentResource);
		addReferenceChannelMember(groupDependentResource);
	}
	
	public void removeChannelMember(ResourcePath id) {
		for (ChannelMember cm: inputChannelMembers) {
			if (cm.getResource() == id) {
				inputChannelMembers.remove(cm);
				super.removeChannelMember(id);
				return;
			}
		}
		for (ChannelMember cm: outputChannelMembers) {
			if (cm.getResource() == id) {
				outputChannelMembers.remove(cm);
				super.removeChannelMember(id);
				return;
			}
		}
		for (ChannelMember cm: referenceChannelMembers) {
			if (cm.getResource() == id) {
				referenceChannelMembers.remove(cm);
				super.removeChannelMember(id);
				return;
			}
		}
	}
	
	public Set<ResourcePath> getInputResources() {
		Set<ResourcePath> inputResources = new HashSet<>();
		for (ChannelMember member: inputChannelMembers) {
			inputResources.add(member.getResource());
		}
		return inputResources;
	}
	
	public Set<ResourcePath> getOutputResources() {
		Set<ResourcePath> outputResources = new HashSet<>();
		for (ChannelMember member: outputChannelMembers) {
			outputResources.add(member.getResource());
		}
		return outputResources;
	}
	
	public Set<ResourcePath> getReferenceResources() {
		Set<ResourcePath> referenceResources = new HashSet<>();
		for (ChannelMember member: referenceChannelMembers) {
			referenceResources.add(member.getResource());
		}
		return referenceResources;
	}
	
	/**
	 * Derive the update expression of the state of the target channel member.
	 * @param targetMember a channel member whose state is to be updated
	 * @return the derived update expression
	 * @throws ParameterizedIdentifierIsFutureWork
	 * @throws ResolvingMultipleDefinitionIsFutureWork
	 * @throws InvalidMessage
	 * @throws UnificationFailed
	 * @throws ValueUndefined
	 */
	public Expression deriveUpdateExpressionOf(ChannelMember targetMember) throws ParameterizedIdentifierIsFutureWork, ResolvingMultipleDefinitionIsFutureWork, InvalidMessage, UnificationFailed, ValueUndefined {
		IResourceStateAccessor defaultStateAccessor = new IResourceStateAccessor() {
			HashMap<String, Parameter> curStateParams = new HashMap<>();
			HashMap<String, Parameter> nextStateParams = new HashMap<>();

			@Override
			public Expression getCurrentStateAccessorFor(ResourcePath target, ResourcePath from) {
				String resource = target.getResourceName();
				Parameter curStateParam = curStateParams.get(resource);
				if (curStateParam == null) {
					curStateParam = new Parameter("cur" + resource);
					curStateParams.put(resource, curStateParam);
				}
				return curStateParam;
			}

			@Override
			public Expression getNextStateAccessorFor(ResourcePath target, ResourcePath from) {
				String resource = target.getResourceName();
				Parameter nextStateParam = nextStateParams.get(resource);
				if (nextStateParam == null) {
					nextStateParam = new Parameter("next" + resource);
					nextStateParams.put(resource, nextStateParam);
				}
				return nextStateParam;
			}
		};
		return deriveUpdateExpressionOf(targetMember, defaultStateAccessor);
	}
	
	/**
	 * Derive the update expression of the state of the target channel member with a given resource state accessor.
	 * @param targetMember a channel member whose state is to be updated
	 * @param stateAccessor a resource state accessor
	 * @return the derived update expression
	 * @throws ParameterizedIdentifierIsFutureWork
	 * @throws ResolvingMultipleDefinitionIsFutureWork
	 * @throws InvalidMessage
	 * @throws UnificationFailed
	 * @throws ValueUndefined
	 */
	public Expression deriveUpdateExpressionOf(ChannelMember targetMember, IResourceStateAccessor stateAccessor) 
			throws ParameterizedIdentifierIsFutureWork, ResolvingMultipleDefinitionIsFutureWork, InvalidMessage, UnificationFailed, ValueUndefined {
		return deriveUpdateExpressionOf(targetMember, stateAccessor, null);
	}

	public Expression deriveUpdateExpressionOf(ChannelMember targetMember, IResourceStateAccessor stateAccessor, HashMap<ResourcePath, IResourceStateAccessor> inputResourceToStateAccessor) 
			throws ParameterizedIdentifierIsFutureWork, ResolvingMultipleDefinitionIsFutureWork, InvalidMessage, UnificationFailed, ValueUndefined {
		if (!getOutputChannelMembers().contains(targetMember)) return null;
		HashSet<Term> messageConstraints = new HashSet<>();
		
		// Calculate message constraints from input state transitions
		for (ChannelMember inputMember: getInputChannelMembers()) {
			ResourcePath inputResource = inputMember.getResource();
			if (inputResource.getNumberOfParameters() > 0) {
				throw new ParameterizedIdentifierIsFutureWork();
			}
			Expression curInputStateAccessor = null;
			Expression nextInputStateAccessor = null;
			if (inputResourceToStateAccessor == null) {
				curInputStateAccessor = stateAccessor.getCurrentStateAccessorFor(inputResource, targetMember.getResource());
				nextInputStateAccessor = stateAccessor.getNextStateAccessorFor(inputResource, targetMember.getResource());
			} else {
				curInputStateAccessor = inputResourceToStateAccessor.get(inputResource).getCurrentStateAccessorFor(inputResource, targetMember.getResource());
				nextInputStateAccessor = inputResourceToStateAccessor.get(inputResource).getNextStateAccessorFor(inputResource, targetMember.getResource());
			}
			Expression messageConstraintByInput = inputMember.getStateTransition().deriveMessageConstraintFor(curInputStateAccessor, nextInputStateAccessor);
			messageConstraints.add((Term) messageConstraintByInput);
		}

		// Calculate message constraints from reference state transitions
		for (ChannelMember referenceMember: getReferenceChannelMembers()) {
			ResourcePath referenceResource = referenceMember.getResource();
			if (referenceResource.getNumberOfParameters() > 0) {
				throw new ParameterizedIdentifierIsFutureWork();
			}
			Expression curInputStateAccessor = null;
			if (inputResourceToStateAccessor == null) {
				curInputStateAccessor = stateAccessor.getCurrentStateAccessorFor(referenceResource, targetMember.getResource());
			} else {
				curInputStateAccessor = inputResourceToStateAccessor.get(referenceResource).getCurrentStateAccessorFor(referenceResource, targetMember.getResource());
			}
			Expression messageConstraintByReference = referenceMember.getStateTransition().deriveMessageConstraintFor(curInputStateAccessor);
			messageConstraints.add((Term) messageConstraintByReference);
		}

		// Unify message constraints
		Term unifiedMessage = null;
		for (Term messageContraint: messageConstraints) {
			if (unifiedMessage == null) {
				unifiedMessage = messageContraint;
			} else {
				unifiedMessage = (Term) unifiedMessage.unify(messageContraint);
				if (unifiedMessage == null) {
					throw new UnificationFailed();
				}
			}
		}
		
		// Calculate the next state of target resource from the unified message and the current resource state
		ResourcePath targetResource = targetMember.getResource();
		if (targetResource.getNumberOfParameters() > 0) {
			throw new ParameterizedIdentifierIsFutureWork();
		}
		Expression curOutputStateAccessor = stateAccessor.getCurrentStateAccessorFor(targetResource, targetResource);
		if (unifiedMessage == null) {
			// for IOChannel
			if (targetMember.getStateTransition().getMessageExpression() instanceof Term) {
				unifiedMessage = (Term) targetMember.getStateTransition().getMessageExpression();
			}
		}
		return targetMember.getStateTransition().deriveNextStateExpressionFor(curOutputStateAccessor, unifiedMessage);
	}
	
	@Override
	public String toString() {
		String channelSource = "channel " + getChannelName() + " {\n";
		for (ChannelMember inputMember: inputChannelMembers) {
			channelSource += "\t in " + inputMember + "\n";
		}
		for (ChannelMember refMember: referenceChannelMembers) {
			channelSource += "\t ref " + refMember + "\n";
		}
		for (ChannelMember outputMember: outputChannelMembers) {
			channelSource += "\t out " + outputMember + "\n";
		}
		channelSource += "}\n";
		return channelSource;
	}
	
	public interface IResourceStateAccessor {
		Expression getCurrentStateAccessorFor(ResourcePath target, ResourcePath from);
		Expression getNextStateAccessorFor(ResourcePath target, ResourcePath from);
	}
}
