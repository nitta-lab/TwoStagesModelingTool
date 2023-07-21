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

public class DataTransferChannelGenerator extends ChannelGenerator {
	protected Set<ChannelMember> inputChannelMembers = null;
	protected Set<ChannelMember> outputChannelMembers = null;
	protected Set<ChannelMember> referenceChannelMembers = null;
	
	public DataTransferChannelGenerator(String channelName) {
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
	
	public void addInputChannelMember(ChannelMember inputChannelMember) {
		inputChannelMembers.add(inputChannelMember);
	}
	
	public Set<ChannelMember> getOutputChannelMembers() {
		return outputChannelMembers;
	}
	
	public void setOutputChannelMembers(Set<ChannelMember> outputChannelMembers) {
		this.outputChannelMembers = outputChannelMembers;
	}
	
	public void addOutputChannelMember(ChannelMember outputChannelMember) {
		outputChannelMembers.add(outputChannelMember);
	}
	
	public Set<ChannelMember> getReferenceChannelMembers() {
		return referenceChannelMembers;
	}
	
	public void setReferenceChannelMembers(Set<ChannelMember> referenceChannelMembers) {
		this.referenceChannelMembers = referenceChannelMembers;
	}
	
	public void addReferenceChannelMember(ChannelMember referenceChannelMember) {
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
	
	public void removeChannelMember(IdentifierTemplate id) {
		for (ChannelMember cm: inputChannelMembers) {
			if (cm.getIdentifierTemplate() == id) {
				inputChannelMembers.remove(cm);
				super.removeChannelMember(id);
				return;
			}
		}
		for (ChannelMember cm: outputChannelMembers) {
			if (cm.getIdentifierTemplate() == id) {
				outputChannelMembers.remove(cm);
				super.removeChannelMember(id);
				return;
			}
		}
		for (ChannelMember cm: referenceChannelMembers) {
			if (cm.getIdentifierTemplate() == id) {
				referenceChannelMembers.remove(cm);
				super.removeChannelMember(id);
				return;
			}
		}
	}
	
	public Set<IdentifierTemplate> getInputIdentifierTemplates() {
		Set<IdentifierTemplate> inputIdentifierTemplates = new HashSet<>();
		for (ChannelMember member: inputChannelMembers) {
			inputIdentifierTemplates.add(member.getIdentifierTemplate());
		}
		return inputIdentifierTemplates;
	}
	
	public Set<IdentifierTemplate> getOutputIdentifierTemplates() {
		Set<IdentifierTemplate> outputIdentifierTemplates = new HashSet<>();
		for (ChannelMember member: outputChannelMembers) {
			outputIdentifierTemplates.add(member.getIdentifierTemplate());
		}
		return outputIdentifierTemplates;
	}
	
	public Set<IdentifierTemplate> getReferenceIdentifierTemplates() {
		Set<IdentifierTemplate> referenceIdentifierTemplates = new HashSet<>();
		for (ChannelMember member: referenceChannelMembers) {
			referenceIdentifierTemplates.add(member.getIdentifierTemplate());
		}
		return referenceIdentifierTemplates;
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
			public Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				String resource = target.getResourceName();
				Parameter curStateParam = curStateParams.get(resource);
				if (curStateParam == null) {
					curStateParam = new Parameter("cur" + resource);
					curStateParams.put(resource, curStateParam);
				}
				return curStateParam;
			}

			@Override
			public Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
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

	public Expression deriveUpdateExpressionOf(ChannelMember targetMember, IResourceStateAccessor stateAccessor, HashMap<IdentifierTemplate, IResourceStateAccessor> inputIdentifierToStateAccessor) 
			throws ParameterizedIdentifierIsFutureWork, ResolvingMultipleDefinitionIsFutureWork, InvalidMessage, UnificationFailed, ValueUndefined {
		if (!getOutputChannelMembers().contains(targetMember)) return null;
		HashSet<Term> messageConstraints = new HashSet<>();
		
		// Calculate message constraints from input state transitions
		for (ChannelMember inputMember: getInputChannelMembers()) {
			IdentifierTemplate inputIdentifier = inputMember.getIdentifierTemplate();
			if (inputIdentifier.getNumberOfParameters() > 0) {
				throw new ParameterizedIdentifierIsFutureWork();
			}
			Expression curInputStateAccessor = null;
			Expression nextInputStateAccessor = null;
			if (inputIdentifierToStateAccessor == null) {
				curInputStateAccessor = stateAccessor.getCurrentStateAccessorFor(inputIdentifier, targetMember.getIdentifierTemplate());
				nextInputStateAccessor = stateAccessor.getNextStateAccessorFor(inputIdentifier, targetMember.getIdentifierTemplate());
			} else {
				curInputStateAccessor = inputIdentifierToStateAccessor.get(inputIdentifier).getCurrentStateAccessorFor(inputIdentifier, targetMember.getIdentifierTemplate());
				nextInputStateAccessor = inputIdentifierToStateAccessor.get(inputIdentifier).getNextStateAccessorFor(inputIdentifier, targetMember.getIdentifierTemplate());
			}
			Expression messageConstraintByInput = inputMember.getStateTransition().deriveMessageConstraintFor(curInputStateAccessor, nextInputStateAccessor);
			messageConstraints.add((Term) messageConstraintByInput);
		}

		// Calculate message constraints from reference state transitions
		for (ChannelMember referenceMember: getReferenceChannelMembers()) {
			IdentifierTemplate referenceIdentifier = referenceMember.getIdentifierTemplate();
			if (referenceIdentifier.getNumberOfParameters() > 0) {
				throw new ParameterizedIdentifierIsFutureWork();
			}
			Expression curInputStateAccessor = null;
			if (inputIdentifierToStateAccessor == null) {
				curInputStateAccessor = stateAccessor.getCurrentStateAccessorFor(referenceIdentifier, targetMember.getIdentifierTemplate());
			} else {
				curInputStateAccessor = inputIdentifierToStateAccessor.get(referenceIdentifier).getCurrentStateAccessorFor(referenceIdentifier, targetMember.getIdentifierTemplate());
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
		IdentifierTemplate targetIdentifier = targetMember.getIdentifierTemplate();
		if (targetIdentifier.getNumberOfParameters() > 0) {
			throw new ParameterizedIdentifierIsFutureWork();
		}
		Expression curOutputStateAccessor = stateAccessor.getCurrentStateAccessorFor(targetIdentifier, targetIdentifier);
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
		Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from);
		Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from);
	}
}
