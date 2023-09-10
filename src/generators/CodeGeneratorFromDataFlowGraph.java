package generators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import code.ast.Block;
import code.ast.CompilationUnit;
import code.ast.FieldDeclaration;
import code.ast.MethodDeclaration;
import code.ast.TypeDeclaration;
import code.ast.VariableDeclaration;
import models.Edge;
import models.Node;
import models.algebra.Expression;
import models.algebra.InvalidMessage;
import models.algebra.ParameterizedIdentifierIsFutureWork;
import models.algebra.Term;
import models.algebra.Type;
import models.algebra.UnificationFailed;
import models.algebra.ValueUndefined;
import models.algebra.Variable;
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.IFlowGraph;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResolvingMultipleDefinitionIsFutureWork;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.StoreAttribute;
import models.dataFlowModel.DataTransferChannel.IResourceStateAccessor;

public class CodeGeneratorFromDataFlowGraph extends CodeGenerator {

	public void generateCodeFromFlowGraph(DataTransferModel model, IFlowGraph flowGraph, ArrayList<Set<Node>> components,
			TypeDeclaration mainComponent, MethodDeclaration mainConstructor, ArrayList<CompilationUnit> codes, ILanguageSpecific langSpec) {
		// For each of other components.
		for (Set<Node> componentNodeSet: components) {
			// Declare this resource.
			Node componentNode = componentNodeSet.iterator().next();
			ResourceNode resourceNode = (ResourceNode) componentNode;
			String resourceName = langSpec.toComponentName(resourceNode.getResource().getResourceName());
			TypeDeclaration component = langSpec.newTypeDeclaration(resourceName);
			
			// Declare the constructor and the fields to refer to other resources.
			List<ResourcePath> depends = new ArrayList<>();
			MethodDeclaration constructor = declareConstructorAndFieldsToReferToResources(resourceNode, component, depends, langSpec);
			
			// Update the main component for this component.
			updateMainComponent(model, mainComponent, mainConstructor, componentNode, depends, langSpec);
			
			ResourcePath res = resourceNode.getResource();
			Type resStateType = res.getResourceStateType();
			
			// Declare the field in this resource to store the state.
			if (((StoreAttribute) resourceNode.getAttribute()).isStored()) {
				FieldDeclaration stateField = langSpec.newFieldDeclaration(resStateType, fieldOfResourceState, langSpec.getFieldInitializer(resStateType, res.getInitialValue()));
				component.addField(stateField);
			}
			
			// Declare the getter method in this resource to obtain the state.
			MethodDeclaration getter = declareGetterMethod(resourceNode, component, resStateType, langSpec);
			
			// Declare the accessor method in the main component to call the getter method.
			declareAccessorInMainComponent(mainComponent, res, langSpec);
			
			// Declare the fields to refer to reference resources.
			declareFieldsToReferenceResources(model, resourceNode, component, constructor, depends, langSpec);
			
			// Declare cache fields and update methods in this resource.
			List<MethodDeclaration> updates = declareCacheFieldsAndUpdateMethods(resourceNode, component, langSpec);
			
			// Declare input methods in this component and the main component.
			List<MethodDeclaration> inputs = declareInputMethodsInThisAndMainComponents(resourceNode, component, mainComponent, model, langSpec);
				
			if (constructor.getParameters() == null) {
				component.removeMethod(constructor);
			}
						
			// Add compilation unit for this component.
			CompilationUnit cu = langSpec.newCompilationUnit(component);
			codes.add(cu);
		}
	}
	
	private MethodDeclaration declareConstructorAndFieldsToReferToResources(ResourceNode resourceNode, TypeDeclaration component, 
			List<ResourcePath> depends, ILanguageSpecific langSpec) {
		// Declare a constructor in each component.
		MethodDeclaration constructor = component.createConstructor();
		Block block = new Block();
		constructor.setBody(block);
		
		// Declare fields in each component. (for data-flow graph)
		for (Edge e: resourceNode.getOutEdges()) {
			if (((PushPullAttribute) ((DataFlowEdge) e).getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
				// for PUSH transfer
				addReference(component, constructor, e.getDestination(), langSpec);
				ResourcePath dstId = ((ResourceNode) e.getDestination()).getResource();
				if (!depends.contains(dstId)) depends.add(dstId);
			}
		}
		for (Edge e: resourceNode.getInEdges()) {
			if (((PushPullAttribute) ((DataFlowEdge) e).getAttribute()).getOptions().get(0) != PushPullValue.PUSH) {
				// for PULL transfer
				addReference(component, constructor, e.getSource(), langSpec);
				ResourcePath srcId = ((ResourceNode) e.getSource()).getResource();
				if (!depends.contains(srcId)) depends.add(srcId);
			}
		}
		return constructor;
	}

	private MethodDeclaration declareGetterMethod(ResourceNode resourceNode, TypeDeclaration component, Type resStateType, ILanguageSpecific langSpec) {
		// Declare the getter method of the resource state.
		MethodDeclaration getter = langSpec.newMethodDeclaration(getterOfResourceState, resStateType);
		component.addMethod(getter);
		
		if (((StoreAttribute) resourceNode.getAttribute()).isStored()) {
			fillGetterMethodToReturnStateField(getter, resStateType, langSpec);
		} else {	
			// invocations to other getter methods when at least one incoming data-flow edges is PULL-style.
			boolean isContainedPush = false;
			DataTransferChannel ch = null;
			HashMap<ResourcePath, IResourceStateAccessor> inputResourceToStateAccessor = new HashMap<>();
			for (Edge eIn: resourceNode.getInEdges()) {
				DataFlowEdge dIn = (DataFlowEdge) eIn;
				if (((PushPullAttribute) dIn.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
					// PUSH transfer
					isContainedPush = true;
					inputResourceToStateAccessor.put(((ResourceNode) dIn.getSource()).getResource(), getPushAccessor());
				} else {
					// PULL transfer
					inputResourceToStateAccessor.put(((ResourceNode) dIn.getSource()).getResource(), getPullAccessor());
					ch = dIn.getChannel();
				}
			}
			// for reference channel members.
			for (ChannelMember c: ch.getReferenceChannelMembers()) {
				inputResourceToStateAccessor.put(c.getResource(), getPullAccessor());			// by pull data transfer
			}
			
			// generate a return statement.
			try {
				for (ChannelMember out: ch.getOutputChannelMembers()) {
					if (out.getResource().equals(resourceNode.getResource())) {
						String[] sideEffects = new String[] {""};
						if (!isContainedPush) {
							// All incoming edges are in PULL-style.
							String curState = ch.deriveUpdateExpressionOf(out, getPullAccessor()).toImplementation(sideEffects);
							getter.addStatement(sideEffects[0] + langSpec.getReturnStatement(curState) + langSpec.getStatementDelimiter());
						} else {
							// At least one incoming edge is in PUSH-style.
							String curState = ch.deriveUpdateExpressionOf(out, getPullAccessor(), inputResourceToStateAccessor).toImplementation(sideEffects);
							getter.addStatement(sideEffects[0] + langSpec.getReturnStatement(curState) + langSpec.getStatementDelimiter());
						}
						break;
					}
				}
			} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork
					| InvalidMessage | UnificationFailed | ValueUndefined e) {
				e.printStackTrace();
			}
		}
		
		return getter;
	}
	
	private List<MethodDeclaration> declareCacheFieldsAndUpdateMethods(ResourceNode resourceNode, TypeDeclaration component, ILanguageSpecific langSpec) {
		// Declare cash fields and update methods in the component.
		String resComponentName = langSpec.toComponentName(resourceNode.getResource().getResourceName());
		List<MethodDeclaration> updateMethods = new ArrayList<>();
		for (Edge e: resourceNode.getInEdges()) {
			DataFlowEdge re = (DataFlowEdge) e;
			ResourcePath srcRes = ((ResourceNode) re.getSource()).getResource();
			String srcResName = srcRes.getResourceName();
			String srcResComponentName = langSpec.toComponentName(srcResName);
			if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
				// for push data transfer
				
				// Declare an update method in the type of the destination resource.
				ArrayList<VariableDeclaration> vars = new ArrayList<>();
				vars.add(langSpec.newVariableDeclaration(srcRes.getResourceStateType(), srcRes.getResourceName()));				
				// For the refs.
				DataTransferChannel ch = (DataTransferChannel) re.getChannel();
				for (ResourcePath ref: ch.getReferenceResources()) {
					if (!ref.equals(resourceNode.getResource())) {
						vars.add(langSpec.newVariableDeclaration(ref.getResourceStateType(), ref.getResourceName()));
					}
				}
				MethodDeclaration update = langSpec.newMethodDeclaration(updateMethodName + srcResComponentName, false, null, vars);
				component.addMethod(update);
				updateMethods.add(update);
				
				// Add a statement to update the state field
				if (((StoreAttribute) resourceNode.getAttribute()).isStored()) {
					try {
						for (ChannelMember out: re.getChannel().getOutputChannelMembers()) {
							if (out.getResource().equals(resourceNode.getResource())) {
								Expression updateExp = re.getChannel().deriveUpdateExpressionOf(out, getPushAccessor());
								String[] sideEffects = new String[] {""};
								String curState = updateExp.toImplementation(sideEffects);
								String updateStatement;
								if (updateExp instanceof Term && ((Term) updateExp).getSymbol().isImplWithSideEffect()) {
									updateStatement = sideEffects[0];
								} else {
									updateStatement = sideEffects[0] + langSpec.getFieldAccessor(fieldOfResourceState) + langSpec.getAssignment() + curState + langSpec.getStatementDelimiter();	// this.value = ...
								}
								if (update.getBody() == null || !update.getBody().getStatements().contains(updateStatement)) {
									update.addFirstStatement(updateStatement);
								}							
								break;
							}
						}
					} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork
							| InvalidMessage | UnificationFailed | ValueUndefined e1) {
						e1.printStackTrace();
					}
				}
				
				// Declare the field to cache the state of the source resource in the type of the destination resource.
				if (resourceNode.getIndegree() > 1) {
					// If incoming edges are multiple
					if (langSpec.declareField()) {
						// Declare the cache field. 
						FieldDeclaration cacheField = langSpec.newFieldDeclaration(
																srcRes.getResourceStateType(), 
																srcRes.getResourceName(), 
																langSpec.getFieldInitializer(srcRes.getResourceStateType(), srcRes.getInitialValue()));
						component.addField(cacheField);
						
					}
					// Update the cache field.
					String cashStatement = langSpec.getFieldAccessor(srcResName) + langSpec.getAssignment() + srcResName + langSpec.getStatementDelimiter();
					if (update.getBody() == null || !update.getBody().getStatements().contains(cashStatement)) {
						update.addFirstStatement(cashStatement);
					}
				}
				
				// Add an invocation to another update method (for a chain of update method invocations).
				for (Edge eOut: resourceNode.getOutEdges()) {
					DataFlowEdge dOut = (DataFlowEdge) eOut;
					if (((PushPullAttribute) dOut.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
						// PUSH transfer
						Map<MethodDeclaration, Set<ResourcePath>> referredResources = new HashMap<>(); 
						List<String> params = new ArrayList<>();
						params.add(langSpec.getFieldAccessor(fieldOfResourceState));
						Set<ResourcePath> referredSet = referredResources.get(update);
						for (ChannelMember rc: re.getChannel().getReferenceChannelMembers()) {
							// to get the value of reference member.
							ResourcePath ref = rc.getResource();
							if (referredSet == null) {
								referredSet = new HashSet<>();
								referredResources.put(update, referredSet);
							}
							if (!ref.equals(resourceNode.getResource())) {
								String refVarName = ref.getResourceName();
								if (!referredSet.contains(ref)) {
									referredSet.add(ref);
									Expression refGetter = getPullAccessor().getCurrentStateAccessorFor(ref, ((ResourceNode) dOut.getSource()).getResource());
									String[] sideEffects = new String[] {""};
									String refExp = refGetter.toImplementation(sideEffects);
									String refTypeName = ref.getResourceStateType().getInterfaceTypeName();
									update.addFirstStatement(sideEffects[0] + langSpec.getVariableDeclaration(refTypeName, refVarName) + langSpec.getAssignment() + refExp + langSpec.getStatementDelimiter());
								}
								params.add(refVarName);
							}
						}
						update.addStatement(langSpec.getMethodInvocation(langSpec.getFieldAccessor(((ResourceNode) dOut.getDestination()).getResource().getResourceName()), 
																			updateMethodName + resComponentName, 
																			params) + langSpec.getStatementDelimiter());	// this.dst.updateSrc(value, refParams);
					}
				}
			}
		}
		return updateMethods;
	}
	
	private List<MethodDeclaration> declareInputMethodsInThisAndMainComponents(ResourceNode resourceNode, TypeDeclaration component,
			TypeDeclaration mainComponent, DataTransferModel model, ILanguageSpecific langSpec) {
		// Declare input methods.
		String resName = resourceNode.getResource().getResourceName();
		String resComponentName = langSpec.toComponentName(resName);
		List<MethodDeclaration> inputMethods = new ArrayList<>();
		for (Channel ch : model.getIOChannels()) {
			for (ChannelMember out : ((DataTransferChannel) ch).getOutputChannelMembers()) {
				if (out.getResource().equals(resourceNode.getResource())) {
					Expression message = out.getStateTransition().getMessageExpression();
					MethodDeclaration input = null;
					MethodDeclaration mainInput = null;
					if (message instanceof Term) {
						// Declare an input method in this component.
						ArrayList<VariableDeclaration> params = new ArrayList<>();
						for (Variable var: message.getVariables().values()) {
							params.add(langSpec.newVariableDeclaration(var.getType(), var.getName()));
						}
						input = langSpec.newMethodDeclaration(((Term) message).getSymbol().getImplName(), false, null, params);
						component.addMethod(input);
						inputMethods.add(input);
						
						// Declare the accessor in the main component to call the input method.
						String str = ((Term) message).getSymbol().getImplName();
						mainInput = getMethod(mainComponent, str);
						if (mainInput == null) {
							mainInput = langSpec.newMethodDeclaration(str, false, null, params);
							mainComponent.addMethod(mainInput);
						} else {
							// Add type to a parameter without type.
							if (mainInput.getParameters() != null) {
								for (VariableDeclaration param: mainInput.getParameters()) {
									if (param.getType() == null) {
										for (VariableDeclaration p: params) {
											if (param.getName().equals(p.getName()) && p.getType() != null) {
												param.setType(p.getType());
											}
										}
									}
								}
							}
						}
					} else if (message instanceof Variable) {
						// Declare an input method in this component.
						input = langSpec.newMethodDeclaration(((Variable) message).getName(), null);
						component.addMethod(input);
						inputMethods.add(input);
						String str = ((Variable) message).getName();
						
						// Declare the accessor in the main component to call the input method.
						mainInput = getMethod(mainComponent, str);
						if (mainInput == null) {
							mainInput = langSpec.newMethodDeclaration(str, null);
							mainComponent.addMethod(mainInput);
						}
					}
					
					// Add an invocation to the accessor method.
					if (mainInput != null) {
						List<String> args = new ArrayList<>();
						if (message instanceof Term) {
							for (Variable var: message.getVariables().values()) {
								args.add(var.getName());
							}
						}
						mainInput.addStatement(langSpec.getMethodInvocation(langSpec.getFieldAccessor(resName), input.getName(), args) + langSpec.getStatementDelimiter());
					}
					
					if (input != null) {
						// Add a statement to update the state field to the input method.
						try {
							String[] sideEffects = new String[] {""};
							Expression updateExp;
							updateExp = ((DataTransferChannel) ch).deriveUpdateExpressionOf(out, getPullAccessor());
							String newState = updateExp.toImplementation(sideEffects);
							String updateStatement;
							if (updateExp instanceof Term && ((Term) updateExp).getSymbol().isImplWithSideEffect()) {
								updateStatement = sideEffects[0];	
							} else {
								updateStatement = sideEffects[0] + langSpec.getFieldAccessor(fieldOfResourceState) + langSpec.getAssignment() + newState + langSpec.getStatementDelimiter();
							}
							input.addFirstStatement(updateStatement);
						} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork
								| InvalidMessage | UnificationFailed | ValueUndefined e) {
							e.printStackTrace();
						}
												
						// Add an invocation to an update method (for a chain of update method invocations).
						for (Edge eOut: resourceNode.getOutEdges()) {
							DataFlowEdge dOut = (DataFlowEdge) eOut;
							if (((PushPullAttribute) dOut.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
								// PUSH transfer
								Map<MethodDeclaration, Set<ResourcePath>> referredResources = new HashMap<>(); 
								List<String> params = new ArrayList<>();
								params.add(langSpec.getFieldAccessor(fieldOfResourceState));
								Set<ResourcePath> referredSet = referredResources.get(input);
								for (ChannelMember rc: ((DataTransferChannel) ch).getReferenceChannelMembers()) {
									// to get the value of reference member.
									ResourcePath ref = rc.getResource();
									if (referredSet == null) {
										referredSet = new HashSet<>();
										referredResources.put(input, referredSet);
									}
									if (!ref.equals(resourceNode.getResource())) {
										String refVarName = ref.getResourceName();
										if (!referredSet.contains(ref)) {
											referredSet.add(ref);
											Expression refGetter = getPullAccessor().getCurrentStateAccessorFor(ref, ((ResourceNode) dOut.getSource()).getResource());
											String[] sideEffects = new String[] {""};
											String refExp = refGetter.toImplementation(sideEffects);
											String refTypeName = ref.getResourceStateType().getInterfaceTypeName();
											input.addFirstStatement(sideEffects[0] + langSpec.getVariableDeclaration(refTypeName, refVarName) + langSpec.getAssignment() + refExp + langSpec.getStatementDelimiter());
										}
										params.add(refVarName);
									}
								}
								input.addStatement(langSpec.getMethodInvocation(langSpec.getFieldAccessor(((ResourceNode) dOut.getDestination()).getResource().getResourceName()), 
																					updateMethodName + resComponentName, 
																					params) + langSpec.getStatementDelimiter());	// this.dst.updateSrc(value, refParams);
							}
						}
					}
				}
			}
		}
		return inputMethods;
	}
}
