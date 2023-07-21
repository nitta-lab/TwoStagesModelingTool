package generators;

import java.util.ArrayList;
import java.util.Arrays;
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
import models.algebra.Constant;
import models.algebra.Expression;
import models.algebra.Field;
import models.algebra.InvalidMessage;
import models.algebra.ParameterizedIdentifierIsFutureWork;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Type;
import models.algebra.UnificationFailed;
import models.algebra.ValueUndefined;
import models.algebra.Variable;
import models.controlFlowModel.ControlFlowGraph;
import models.controlFlowModel.EntryPointObjectNode;
import models.controlFlowModel.ObjectNode;
import models.controlFlowModel.StatefulObjectNode;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.IdentifierTemplate;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataTransferChannelGenerator;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.IFlowGraph;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResolvingMultipleDefinitionIsFutureWork;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.StoreAttribute;
import models.dataFlowModel.DataTransferChannelGenerator.IResourceStateAccessor;

public class CodeGeneratorFromControlFlowGraph extends CodeGenerator {

	@Override
	public void generateCodeFromFlowGraph(DataTransferModel model, IFlowGraph flowGraph, ArrayList<Node> components,
			TypeDeclaration mainComponent, MethodDeclaration mainConstructor, ArrayList<CompilationUnit> codes, ILanguageSpecific langSpec) {
		// Reconstruct data-flow information.
		Map<Edge, Map<PushPullValue, List<ResourceNode>>> dataFlowInform = new HashMap<>();
		ControlFlowGraph controlFlowGraph = (ControlFlowGraph) flowGraph;
		for (Node root: controlFlowGraph.getPushCallGraph().getRootNodes()) {
			Set<ResourceNode> treeResources = traverseCallTree(root, new HashSet<>());
			annotateDataFlowAttributes(root, dataFlowInform, treeResources, new ArrayList<>());
			removeRedundantAttributes(root, dataFlowInform);
		}
		for (Node root: controlFlowGraph.getPullCallGraph().getRootNodes()) {
			Set<ResourceNode> treeResources = traverseCallTree(root, new HashSet<>());
			annotateDataFlowAttributes(root, dataFlowInform, treeResources, new ArrayList<>());
			removeRedundantAttributes(root, dataFlowInform);
		}
		
		// For each of other components.
		Map<Node, TypeDeclaration> componentMap = new HashMap<>();
		for (Node componentNode: components) {
			// Declare this component.
			String componentName = langSpec.toComponentName(((ObjectNode) componentNode).getName());
			TypeDeclaration component = langSpec.newTypeDeclaration(componentName);
			componentMap.put(componentNode, component);
			
			// Declare the constructor and the fields to refer to the callee components.
			List<IdentifierTemplate> depends = new ArrayList<>();
			MethodDeclaration constructor = declareConstructorAndFieldsToCalleeComponents((ObjectNode) componentNode, component, depends, langSpec);
			
			if (componentNode instanceof StatefulObjectNode) {
				// For this resource.
				ResourceNode resourceNode = ((StatefulObjectNode) componentNode).getResource();
				IdentifierTemplate resId = resourceNode.getIdentifierTemplate();
				Type resStateType = resId.getResourceStateType();
				
				// Declare the field in this resource to store the state.
				if (((StoreAttribute) resourceNode.getAttribute()).isStored()) {
					FieldDeclaration stateField = langSpec.newFieldDeclaration(resStateType, fieldOfResourceState, langSpec.getFieldInitializer(resStateType, resId.getInitialValue()));
					component.addField(stateField);
				}
				
				// Declare the accessor method in the main component to call the getter method.
				declareAccessorInMainComponent(mainComponent, resId, langSpec);
				
				// Declare the fields to refer to reference resources.
				declareFieldsToReferenceResources(model, resourceNode, component, constructor, depends, langSpec);				
			}
			
			// Update the main component for this component.
			updateMainComponent(model, mainComponent, mainConstructor, componentNode, depends, langSpec);
			if (constructor.getParameters() == null) {
				component.removeMethod(constructor);
			}
						
			// Add compilation unit for this component.
			CompilationUnit cu = langSpec.newCompilationUnit(component);
			codes.add(cu);
		}
		
		// Declare and Fill the getter method to return the resource state.
		for (Node node: controlFlowGraph.getPushCallGraph().getNodes()) {
			TypeDeclaration component = componentMap.get(node);
			if (node instanceof StatefulObjectNode) {
				ResourceNode resourceNode = ((StatefulObjectNode) node).getResource();
				Type resStateType = resourceNode.getIdentifierTemplate().getResourceStateType();				
				if (((StoreAttribute) resourceNode.getAttribute()).isStored()) {
					// Declare the getter method in this resource to obtain the state.
					MethodDeclaration getter = langSpec.newMethodDeclaration(getterOfResourceState, resStateType);
					component.addMethod(getter);
					fillGetterMethodToReturnStateField(getter, resourceNode.getIdentifierTemplate().getResourceStateType(), langSpec);		// return this.value;
				}
			}
		}
		
		for (Node node: controlFlowGraph.getPullCallGraph().getNodes()) {
			String nodeName = ((ObjectNode) node).getName();
			if (componentMap.get(node) == null) {
				for (Node node2: componentMap.keySet()) {
					if (((ObjectNode) node2).getName().equals(nodeName)) {
						componentMap.put(node, componentMap.get(node2));		// Since nodes shared by PUSH and PULL call graphs are duplicated.
						break;
					}
				}
			}
		}
		
		// Declare other getter methods.
		for (Node root: controlFlowGraph.getPullCallGraph().getRootNodes()) {
			MethodDeclaration getter = declareAndFillGetterMethods(root, null, dataFlowInform, componentMap, langSpec);			
		}
		
		// Declare update and input methods.
		for (Node root: controlFlowGraph.getPushCallGraph().getRootNodes()) {
			MethodDeclaration input = declareAndFillUpdateAndInputMethods(root, null, null, dataFlowInform, componentMap, langSpec);
			mainComponent.addMethod(input);
		}
	}

	private Set<ResourceNode> traverseCallTree(Node node, Set<ResourceNode> visited) {
		if (node instanceof StatefulObjectNode) {
			ResourceNode resNode = ((StatefulObjectNode) node).getResource();
			visited.add(resNode);
		}
		// Traverse the call tree.
		for (Edge e: node.getOutEdges()) {
			visited = traverseCallTree(e.getDestination(), visited);
		}
		return visited;
	}

	private void annotateDataFlowAttributes(Node node, Map<Edge, Map<PushPullValue, List<ResourceNode>>> dataFlowInform, Set<ResourceNode> resourceNodes, List<Edge> path) {
		if (node instanceof StatefulObjectNode) {
			// Add data-flow attributes to the path to node.
			ResourceNode resNode = ((StatefulObjectNode) node).getResource();
			for (Edge outE: resNode.getOutEdges()) {
				// If resNode is the source of data-flow.
				ResourceNode dstOfDataFlowNode = (ResourceNode) outE.getDestination();
				if (resourceNodes.contains(dstOfDataFlowNode)) {
					// If the data transfer is closed within this call tree.
					for (Edge e: path) {
						// Add pull attributes to the path to resNode.
						Map<PushPullValue, List<ResourceNode>> edgeAttributes = dataFlowInform.get(e);
						if (edgeAttributes == null) {
							edgeAttributes = new HashMap<>();
							dataFlowInform.put(e, edgeAttributes);
						}
						List<ResourceNode> pullSrcs = edgeAttributes.get(PushPullValue.PULL);
						if (pullSrcs == null) {
							pullSrcs = new ArrayList<>();
							edgeAttributes.put(PushPullValue.PULL, pullSrcs);
						}
						pullSrcs.add(resNode);
					}
				}
			}
			for (Edge inE: resNode.getInEdges()) {
				// If resNode is a destination of data-flow.
				ResourceNode srcOfDataFlowNode = (ResourceNode) inE.getSource();
				if (resourceNodes.contains(srcOfDataFlowNode)) {
					// If the data transfer is closed done within this call tree.
					for (Edge e: path) {
						// Add push attributes to the path to resNode.
						Map<PushPullValue, List<ResourceNode>> edgeAttributes = dataFlowInform.get(e);
						if (edgeAttributes == null) {
							edgeAttributes = new HashMap<>();
							dataFlowInform.put(e, edgeAttributes);
						}
						List<ResourceNode> pushSrcs = edgeAttributes.get(PushPullValue.PUSH);
						if (pushSrcs == null) {
							pushSrcs = new ArrayList<>();
							edgeAttributes.put(PushPullValue.PUSH, pushSrcs);
						}
						pushSrcs.add(srcOfDataFlowNode);
					}
				}
			}
		}
		// Traverse the call tree.
		for (Edge e: node.getOutEdges()) {
			path.add(e);
			annotateDataFlowAttributes(e.getDestination(), dataFlowInform, resourceNodes, path);
			path.remove(e);
		}
	}
	
	private void removeRedundantAttributes(Node node, Map<Edge, Map<PushPullValue, List<ResourceNode>>> dataFlowInform) {
		// Traverse the call tree.
		for (Edge e: node.getOutEdges()) {
			// Remove attributes that are common to PUSH and PULL.
			if (dataFlowInform.get(e) == null) {
				dataFlowInform.put(e, new HashMap<>());
			}
			List<ResourceNode> pushFlows = dataFlowInform.get(e).get(PushPullValue.PUSH);
			List<ResourceNode> pullFlows = dataFlowInform.get(e).get(PushPullValue.PULL);
			if (pushFlows == null) {
				pushFlows = new ArrayList<>();
			}
			if (pullFlows == null) {
				pullFlows = new ArrayList<>();
			}
			List<ResourceNode> pushFlowsOrg = new ArrayList<>(pushFlows);
			for (ResourceNode r: pullFlows) {
				pushFlows.remove(r);
			}
			for (ResourceNode r: pushFlowsOrg) {
				pullFlows.remove(r);
			}
			pushFlows = new ArrayList<>(new HashSet<>(pushFlows));
			pullFlows = new ArrayList<>(new HashSet<>(pullFlows));
			dataFlowInform.get(e).put(PushPullValue.PUSH, pushFlows);
			dataFlowInform.get(e).put(PushPullValue.PULL, pullFlows);
			removeRedundantAttributes(e.getDestination(), dataFlowInform);
		}
	}
	
	private MethodDeclaration declareConstructorAndFieldsToCalleeComponents(ObjectNode componentNode, TypeDeclaration component, 
			List<IdentifierTemplate> depends, ILanguageSpecific langSpec) {
		// Declare a constructor in each component.
		MethodDeclaration constructor = component.createConstructor();
		Block block = new Block();
		constructor.setBody(block);
		
		// Declare fields in each component. (for control-flow graph)
		for (Edge e: componentNode.getOutEdges()) {
			ObjectNode dstNode = (ObjectNode) e.getDestination();
			addReference(component, constructor, dstNode, langSpec);
			if (dstNode instanceof StatefulObjectNode) {
				IdentifierTemplate dstId = ((StatefulObjectNode) dstNode).getResource().getIdentifierTemplate();
				if (!depends.contains(dstId)) depends.add(dstId);
			}
		}
		return constructor;
	}
	

	private MethodDeclaration declareAndFillGetterMethods(Node node, Edge inEdge, 
			Map<Edge, Map<PushPullValue, List<ResourceNode>>> dataFlowInform, Map<Node, TypeDeclaration> componentMap,
			ILanguageSpecific langSpec) {
		TypeDeclaration component = componentMap.get(node);
		List<ResourceNode> resourcesToReturn = null;
		if (inEdge != null) {
			resourcesToReturn = dataFlowInform.get(inEdge).get(PushPullValue.PULL);
		}
		if (node instanceof StatefulObjectNode) {
			ResourceNode resourceNode = ((StatefulObjectNode) node).getResource();
			Type resStateType = resourceNode.getIdentifierTemplate().getResourceStateType();				
			MethodDeclaration getter = langSpec.newMethodDeclaration(getterOfResourceState, resStateType);
			MethodDeclaration getter2 = getMethod(component, getter.getName());
			if (getter2 == null) {
				component.addMethod(getter);
			} else {
				getter = getter2;
			}
			if (((StoreAttribute) resourceNode.getAttribute()).isStored()) {
				if (getter2 == null) {
					// Declare the getter method in this resource to obtain the state.
					fillGetterMethodToReturnStateField(getter, resourceNode.getIdentifierTemplate().getResourceStateType(), langSpec);		// return this.value;
				}
			} else {
				// Invocations to other getter methods when at least one incoming data-flow edges is PULL-style.
				boolean isContainedPush = false;
				DataTransferChannelGenerator ch = null;
				HashMap<IdentifierTemplate, IResourceStateAccessor> inputIdentifierToStateAccessor = new HashMap<>();
				for (Edge eIn: resourceNode.getInEdges()) {
					DataFlowEdge dIn = (DataFlowEdge) eIn;
					if (((PushPullAttribute) dIn.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
						// PUSH data transfer
						isContainedPush = true;
						inputIdentifierToStateAccessor.put(((ResourceNode) dIn.getSource()).getIdentifierTemplate(), getPushAccessor());
					} else {
						// PULL data transfer
						for (Edge outEdge: node.getOutEdges()) {
							// For each call edge.
							ObjectNode dstNode = (ObjectNode) outEdge.getDestination();
							List<ResourceNode> returnedResources = dataFlowInform.get(outEdge).get(PushPullValue.PULL);
							if (returnedResources.contains((ResourceNode) dIn.getSource())) {
								if (returnedResources.size() == 1) {
									MethodDeclaration nextGetter = declareAndFillGetterMethods(dstNode, outEdge, dataFlowInform, componentMap, langSpec);
									inputIdentifierToStateAccessor.put(((ResourceNode) dIn.getSource()).getIdentifierTemplate(), getPullAccessor(dstNode.getName(), nextGetter.getName()));
									break;
								} else {
									MethodDeclaration nextGetter = declareAndFillGetterMethods(dstNode, outEdge, dataFlowInform, componentMap, langSpec);
									int idx = returnedResources.indexOf((ResourceNode) dIn.getSource());
									int len = returnedResources.size();
									inputIdentifierToStateAccessor.put(((ResourceNode) dIn.getSource()).getIdentifierTemplate(), 
											getPullAccessor(langSpec.getTupleGet(langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNode.getName()), nextGetter.getName()), idx, len)));
									break;
								}
							}
						}
						ch = dIn.getChannelGenerator();		// Always unique.
					}
				}
				// For reference channel members.
				for (ChannelMember c: ch.getReferenceChannelMembers()) {
					inputIdentifierToStateAccessor.put(c.getIdentifierTemplate(), getPullAccessor());			// by pull data transfer
				}
				
				// Add a return statement.
				try {
					for (ChannelMember out: ch.getOutputChannelMembers()) {
						if (out.getIdentifierTemplate() == resourceNode.getIdentifierTemplate()) {
							String[] sideEffects = new String[] {""};
							// The following process is common to the cases of 1) and 2).
							// 1) All incoming edges are in PULL-style.
							// 2) At least one incoming edge is in PUSH-style.
							String curState = ch.deriveUpdateExpressionOf(out, getPullAccessor(), inputIdentifierToStateAccessor).toImplementation(sideEffects);
							getter.addStatement(sideEffects[0] + langSpec.getReturnStatement(curState) + langSpec.getStatementDelimiter());
							break;
						}
					}
				} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork
						| InvalidMessage | UnificationFailed | ValueUndefined e) {
					e.printStackTrace();
				}
			}
			if (resourcesToReturn == null || resourcesToReturn.size() == 1) return getter;
		} else if (resourcesToReturn == null || resourcesToReturn.size() == 1) {
			// Declare a mediate getter method to return a single value.
			String getterMethodName = "get";
			ResourceNode returnedRes = null; 
			if (resourcesToReturn != null) {
				returnedRes = resourcesToReturn.get(0);
			} else {
				// Unexpected.
			}
			getterMethodName += langSpec.toComponentName(returnedRes.getIdentifierTemplate().getResourceName()) + "Value";
			MethodDeclaration mediateGetter = langSpec.newMethodDeclaration(getterMethodName, returnedRes.getIdentifierTemplate().getResourceStateType());
			component.addMethod(mediateGetter);
			
			// Add a return statement.
			if (node.getOutdegree() == 1) {
				Edge outEdge = node.getOutEdges().iterator().next();
				ObjectNode dstNode = (ObjectNode) outEdge.getDestination();
				MethodDeclaration nextGetter = declareAndFillGetterMethods(dstNode, outEdge, dataFlowInform, componentMap, langSpec);
				mediateGetter.addStatement(
						langSpec.getReturnStatement(langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNode.getName()), nextGetter.getName()))
						+ langSpec.getStatementDelimiter());
			} else {
				// Unexpected.
			}
			return mediateGetter;
		}
		// Declare a mediate getter method to return multiple values.
		String getterMethodName = "get";
		for (ResourceNode rn: resourcesToReturn) {
			getterMethodName += langSpec.toComponentName(rn.getIdentifierTemplate().getResourceName());
		}
		getterMethodName += "Values";
		Type returnType = createReturnType(resourcesToReturn, langSpec);
		MethodDeclaration mediateGetter = langSpec.newMethodDeclaration(getterMethodName, returnType);
		component.addMethod(mediateGetter);
		
		// Add a return statement.
		if (node.getOutdegree() == 1 && resourcesToReturn != null
				&& resourcesToReturn.equals(dataFlowInform.get(node.getOutEdges().iterator().next()).get(PushPullValue.PULL))) {
			// Directly returns the returned value.
			Edge outEdge = node.getOutEdges().iterator().next();
			ObjectNode dstNode = (ObjectNode) outEdge.getDestination();
			MethodDeclaration nextGetter = declareAndFillGetterMethods(dstNode, outEdge, dataFlowInform, componentMap, langSpec);
			String getterInvocation = langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNode.getName()), nextGetter.getName());
			mediateGetter.addStatement(langSpec.getReturnStatement(getterInvocation) + langSpec.getStatementDelimiter());
		} else {
			List<String> params = new ArrayList<>();
			for (ResourceNode rn: resourcesToReturn) {
				IdentifierTemplate rId = rn.getIdentifierTemplate();
				if (rId.getResourceName().equals(((ObjectNode) node).getName())) {
					params.add(langSpec.getMethodInvocation(getterOfResourceState));
				} else {
					for (Edge outEdge: node.getOutEdges()) {
						ObjectNode dstNode = (ObjectNode) outEdge.getDestination();
						List<ResourceNode> returnedResources = dataFlowInform.get(outEdge).get(PushPullValue.PULL);
						if (returnedResources.contains(rn)) {
							if (returnedResources.size() == 1) {
								MethodDeclaration nextGetter = declareAndFillGetterMethods(dstNode, outEdge, dataFlowInform, componentMap, langSpec);
								params.add(langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNode.getName()), nextGetter.getName()));
							} else {
								MethodDeclaration nextGetter = declareAndFillGetterMethods(dstNode, outEdge, dataFlowInform, componentMap, langSpec);
								int idx = returnedResources.indexOf(rn);
								int len = returnedResources.size();
								params.add(langSpec.getTupleGet(langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNode.getName()), nextGetter.getName()), idx, len));
							}
							break;
						}
					}
				}
			}
			mediateGetter.addStatement(
					langSpec.getReturnStatement(langSpec.getConstructorInvocation(returnType.getImplementationTypeName(), params))
					+ langSpec.getStatementDelimiter());
		}
		return mediateGetter;
	}

	private MethodDeclaration declareAndFillUpdateAndInputMethods(Node node, Edge inEdge, Node prevResNode, 
			Map<Edge, Map<PushPullValue, List<ResourceNode>>> dataFlowInform, Map<Node, TypeDeclaration> componentMap, ILanguageSpecific langSpec) {
		TypeDeclaration component = componentMap.get(node);
		List<ResourceNode> resourcesToReturn = null; 
		List<ResourceNode> resourcesToReceive = null;
		if (dataFlowInform.get(inEdge) != null) {
			resourcesToReturn = dataFlowInform.get(inEdge).get(PushPullValue.PULL);
			resourcesToReceive = dataFlowInform.get(inEdge).get(PushPullValue.PUSH);
		}
		if (node instanceof StatefulObjectNode) {
			// Declare update or input method in the resource component.
			ResourceNode resourceNode = ((StatefulObjectNode) node).getResource();
			MethodDeclaration updateOrInput = null;
			if (!(prevResNode instanceof EntryPointObjectNode) 
					|| (resourcesToReceive != null && resourcesToReceive.size() > 0)) {
				updateOrInput = getUpdateMethod(inEdge, component, dataFlowInform, langSpec);
				if (updateOrInput != null) return updateOrInput;
				// Declare an update method.
				updateOrInput = declareUpdateMethod(node, inEdge, component, dataFlowInform, langSpec);
			} else {
				DataTransferChannelGenerator ch = ((EntryPointObjectNode) prevResNode).getIoChannelGenerator();
				updateOrInput = getInputMethod(resourceNode, ch, component);
				if (updateOrInput != null) return updateOrInput;
				// Declare an input method.
				updateOrInput = declareInputMethod(resourceNode, ch, langSpec);
			}
			component.addMethod(updateOrInput);
			
			Map<ResourceNode, String> resToVar = new HashMap<>();
			Map<String, List<ResourceNode>> varToRes = new HashMap<>();
			for (Edge outEdge: node.getOutEdges()) {
				Node dstNode = outEdge.getDestination();
				MethodDeclaration calleeMethod = declareAndFillUpdateAndInputMethods(dstNode, outEdge, node, dataFlowInform, componentMap, langSpec);
				// Add a statement to call the destination method.
				List<ResourceNode> returnedResources = dataFlowInform.get(outEdge).get(PushPullValue.PULL);
				String varName = addInvocationInResourceUpdate(node, updateOrInput, calleeMethod, ((ObjectNode) dstNode).getName(), returnedResources, langSpec);
				if (varName != null && returnedResources != null) {
					for (ResourceNode rn: returnedResources) {
						String resName = rn.getIdentifierTemplate().getResourceName();
						resToVar.put(rn, resName);
						varToRes.put(resName, Arrays.asList(new ResourceNode[] {rn}));
					}
//					// Alternative implementation.
//					varToRes.put(varName, returnedResources);
//					for (ResourceNode rn: returnedResources) {
//						resToVar.put(rn, varName);
//					}
				}
			}
			
			if (resourcesToReturn != null && resourcesToReturn.size() > 0) {
				// Set the return type and add a return statement.
				Type returnType = createReturnType(resourcesToReturn, langSpec);		
				updateOrInput.setReturnType(returnType);
				String returnValue = createReturnValue(resourcesToReturn, node, returnType, resToVar, varToRes, langSpec);
				updateOrInput.addStatement(langSpec.getReturnStatement(returnValue) + langSpec.getStatementDelimiter());
			}
			return updateOrInput;
		} else if (node instanceof EntryPointObjectNode) {
			// Declare an input method.
			MethodDeclaration input = null;
			for (Edge outEdge: node.getOutEdges()) {
				Node dstNode = outEdge.getDestination();
				MethodDeclaration calleeMethod = declareAndFillUpdateAndInputMethods(dstNode, outEdge, node, dataFlowInform, componentMap, langSpec);
				if (input == null) {
					// Declare an input method.
					if (calleeMethod.getParameters() != null) {
						input  = langSpec.newMethodDeclaration(calleeMethod.getName(), false, null, new ArrayList<>(calleeMethod.getParameters()));
					} else {
						input  = langSpec.newMethodDeclaration(calleeMethod.getName(), null);
					}
				}
				// Add a statement to call the destination method.
				String varName = addInvocationInMediatorUpdate(input, calleeMethod, ((ObjectNode) dstNode).getName(), dataFlowInform.get(outEdge).get(PushPullValue.PULL), langSpec);
			}
			return input;
		} else {
			// Declare update or input method in the mediate component.
			List<MethodDeclaration> updateMethods = getUpdateMethods(component);
			if (updateMethods.size() > 0) return updateMethods.get(0);
			MethodDeclaration updateOrInput = null;
			if (!(prevResNode instanceof EntryPointObjectNode)
					|| (resourcesToReceive != null && resourcesToReceive.size() > 0)) {
				// Declare an update method.
				updateOrInput = declareUpdateMethod(node, inEdge, component, dataFlowInform, langSpec);
				component.addMethod(updateOrInput);
			}
			
			if (node.getOutdegree() == 1 && resourcesToReturn != null 
					&& resourcesToReturn.equals(dataFlowInform.get(node.getOutEdges().iterator().next()).get(PushPullValue.PULL))) {			
				// Directly returns the returned value.
				Edge outEdge = node.getOutEdges().iterator().next();
				ObjectNode dstNode = (ObjectNode) outEdge.getDestination();
				MethodDeclaration calleeMethod = declareAndFillUpdateAndInputMethods(dstNode, outEdge, prevResNode, dataFlowInform, componentMap, langSpec);
				if (updateOrInput == null && prevResNode instanceof EntryPointObjectNode) {
					// Declare an input method.
					if (calleeMethod.getParameters() != null) {
						updateOrInput  = langSpec.newMethodDeclaration(calleeMethod.getName(), false, null, new ArrayList<>(calleeMethod.getParameters()));
					} else {
						updateOrInput  = langSpec.newMethodDeclaration(calleeMethod.getName(), null);
					}
					component.addMethod(updateOrInput);
				}
				// Set the return type and add a return statement.
				updateOrInput.setReturnType(calleeMethod.getReturnType());
				String updateInvocation = langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNode.getName()), calleeMethod.getName());
				updateOrInput.addStatement(langSpec.getReturnStatement(updateInvocation) + langSpec.getStatementDelimiter());
			} else {
				Map<ResourceNode, String> resToVar = new HashMap<>();
				Map<String, List<ResourceNode>> varToRes = new HashMap<>();
				for (Edge outEdge: node.getOutEdges()) {
					Node dstNode = outEdge.getDestination();
					MethodDeclaration calleeMethod = declareAndFillUpdateAndInputMethods(dstNode, outEdge, prevResNode, dataFlowInform, componentMap, langSpec);
					if (updateOrInput == null && prevResNode instanceof EntryPointObjectNode) {
						// Declare an input method.
						if (calleeMethod.getParameters() != null) {
							updateOrInput  = langSpec.newMethodDeclaration(calleeMethod.getName(), false, null, new ArrayList<>(calleeMethod.getParameters()));
						} else {
							updateOrInput  = langSpec.newMethodDeclaration(calleeMethod.getName(), null);
						}
						component.addMethod(updateOrInput);
					}
					// Add a statement to call the destination method.
					List<ResourceNode> returnedResources = dataFlowInform.get(outEdge).get(PushPullValue.PULL);
					String varName = addInvocationInMediatorUpdate(updateOrInput, calleeMethod, ((ObjectNode) dstNode).getName(), returnedResources, langSpec);
					if (varName != null && returnedResources != null) {
						for (ResourceNode rn: returnedResources) {
							String resName = rn.getIdentifierTemplate().getResourceName();
							resToVar.put(rn, resName);
							varToRes.put(resName, Arrays.asList(new ResourceNode[] {rn}));
						}
//						// Alternative implementation.
//						varToRes.put(varName, returnedResources);
//						for (ResourceNode rn: returnedResources) {
//							resToVar.put(rn, varName);
//						}
					}
				}
				if (resourcesToReturn != null && resourcesToReturn.size() > 0) {
					// Set the return type and add a return statement.
					Type returnType = createReturnType(resourcesToReturn, langSpec);		
					updateOrInput.setReturnType(returnType);
					String returnValue = createReturnValue(resourcesToReturn, node, returnType, resToVar, varToRes, langSpec);
					updateOrInput.addStatement(langSpec.getReturnStatement(returnValue) + langSpec.getStatementDelimiter());
				}
			} 
			return updateOrInput;
		}
	}

	private MethodDeclaration declareUpdateMethod(Node node, Edge inEdge, TypeDeclaration component,
			Map<Edge, Map<PushPullValue, List<ResourceNode>>> dataFlowInform, ILanguageSpecific langSpec) {
		// Declare an update method in the component.
		ArrayList<VariableDeclaration> vars = new ArrayList<>();
		List<ResourceNode> passedResoueces = dataFlowInform.get(inEdge).get(PushPullValue.PUSH);
		Set<IdentifierTemplate> passedIds = new HashSet<>();
		String methodName = updateMethodName;
		for (ResourceNode rn: passedResoueces) {
			IdentifierTemplate rId = rn.getIdentifierTemplate();
			passedIds.add(rId);
			methodName += langSpec.toComponentName(rId.getResourceName());
			vars.add(langSpec.newVariableDeclaration(rId.getResourceStateType(), rId.getResourceName()));				
		}
		MethodDeclaration update = langSpec.newMethodDeclaration(methodName, false, null, vars);
		
		if (node instanceof StatefulObjectNode) {
			// Add a statement to update the state field
			ResourceNode resourceNode = ((StatefulObjectNode) node).getResource();
			if (((StoreAttribute) resourceNode.getAttribute()).isStored()) {
				try {
					for (Edge e: resourceNode.getInEdges()) {
						DataFlowEdge re = (DataFlowEdge) e;
						for (ChannelMember in: re.getChannelGenerator().getInputChannelMembers()) {
							if (passedIds.contains(in.getIdentifierTemplate())) {
								for (ChannelMember out: re.getChannelGenerator().getOutputChannelMembers()) {
									if (out.getIdentifierTemplate() == resourceNode.getIdentifierTemplate()) {
										Expression updateExp = re.getChannelGenerator().deriveUpdateExpressionOf(out, getPushAccessor());
										String[] sideEffects = new String[] {""};
										String curState = updateExp.toImplementation(sideEffects);
										String updateStatement;
										if (updateExp instanceof Term && ((Term) updateExp).getSymbol().isImplWithSideEffect()) {
											updateStatement = sideEffects[0];
										} else {
											updateStatement = sideEffects[0] + langSpec.getFieldAccessor(fieldOfResourceState) + langSpec.getAssignment() + curState + langSpec.getStatementDelimiter();	// this.value = ...
										}
										update.addFirstStatement(updateStatement);
										break;
									}
								}
							}
						}
					}
				} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork
						| InvalidMessage | UnificationFailed | ValueUndefined e1) {
					e1.printStackTrace();
				}
			}
			
			// Declare the field to cache the state of the source resource in the type of the destination resource.
			if (node.getIndegree() > 1) {
				// If incoming edges are multiple
				for (IdentifierTemplate srcRes: passedIds) {
					String srcResName = srcRes.getResourceName();
					if (langSpec.declareField()) {
						// Declare the cache field. 
						FieldDeclaration cacheField = langSpec.newFieldDeclaration(
																srcRes.getResourceStateType(), 
																srcResName, 
																langSpec.getFieldInitializer(srcRes.getResourceStateType(), srcRes.getInitialValue()));
						component.addField(cacheField);
						
					}
					// Update the cache field.
					String cashStatement = langSpec.getFieldAccessor(srcResName) + langSpec.getAssignment() + srcResName + langSpec.getStatementDelimiter();
					update.addFirstStatement(cashStatement);
				}
			}
		}
		return update;
	}

	private MethodDeclaration declareInputMethod(ResourceNode resourceNode, DataTransferChannelGenerator ch, ILanguageSpecific langSpec) {
		MethodDeclaration input = null;
		for (ChannelMember out : ch.getOutputChannelMembers()) {
			if (out.getIdentifierTemplate().equals(resourceNode.getIdentifierTemplate())) {
				Expression message = out.getStateTransition().getMessageExpression();
				if (message instanceof Term) {
					// Declare an input method in this component.
					ArrayList<VariableDeclaration> params = new ArrayList<>();
					for (Variable var: message.getVariables().values()) {
						params.add(langSpec.newVariableDeclaration(var.getType(), var.getName()));
					}
					input = langSpec.newMethodDeclaration(((Term) message).getSymbol().getImplName(), false, null, params);
				} else if (message instanceof Variable) {
					// Declare an input method in this component.
					input = langSpec.newMethodDeclaration(((Variable) message).getName(), null);
				}
				
				if (input != null) {
					// Add a statement to update the state field to the input method.
					try {
						String[] sideEffects = new String[] {""};
						Expression updateExp;
						updateExp = ch.deriveUpdateExpressionOf(out, getPullAccessor());
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
				}
				break;
			}
		}
		return input;
	}

	private String createReturnValue(List<ResourceNode> resourcesToReturn, Node node, Type returnType, Map<ResourceNode, String> resToVar, Map<String, List<ResourceNode>> varToRes, ILanguageSpecific langSpec) {
		List<String> params = new ArrayList<>();
		for (ResourceNode rn: resourcesToReturn) {
			IdentifierTemplate rId = rn.getIdentifierTemplate();
			if (rId.getResourceName().equals(((ObjectNode) node).getName())) {
				params.add(langSpec.getFieldAccessor(fieldOfResourceState));
			} else {
				String varName = resToVar.get(rn);
				if (varToRes.get(varName).size() == 1) {
					params.add(varName);
				} else {
					params.add(langSpec.getTupleGet(varName, varToRes.get(varName).indexOf(rn), varToRes.get(varName).size()));
				}
			}
		}
		if (params.size() == 1) {
			return params.iterator().next();
		} else {
			return langSpec.getConstructorInvocation(returnType.getImplementationTypeName(), params);
		}
	}

	private Type createReturnType(List<ResourceNode> resourcesToReturn, ILanguageSpecific langSpec) {
		if (resourcesToReturn.size() == 1) {
			return resourcesToReturn.iterator().next().getIdentifierTemplate().getResourceStateType();
		}
		List<Type> compTypes = new ArrayList<>();
		for (ResourceNode rn: resourcesToReturn) {
			IdentifierTemplate rId = rn.getIdentifierTemplate();
			compTypes.add(rId.getResourceStateType());
		}
		Type returnType = langSpec.newTupleType(compTypes);
		return returnType;
	}
	

	private String addInvocationInResourceUpdate(Node node, MethodDeclaration resourceUpdateMethod, MethodDeclaration calleeMethod, String dstNodeName, List<ResourceNode> returnResources, ILanguageSpecific langSpec) {
		List<String> params = new ArrayList<>();
		params.add(langSpec.getFieldAccessor(fieldOfResourceState));
		if (calleeMethod.getParameters() != null) {
			for (VariableDeclaration v: calleeMethod.getParameters()) {
				if (!((ObjectNode) node).getName().equals(v.getName())) {
					params.add(v.getName());
				}
			}
		}
//		for (ChannelMember rc: re.getChannelGenerator().getReferenceChannelMembers()) {
//			// to get the value of reference member.
//			IdentifierTemplate ref = rc.getIdentifierTemplate();
//			if (referredSet == null) {
//				referredSet = new HashSet<>();
//				referredResources.put(update, referredSet);
//			}
//			if (ref != resourceNode.getIdentifierTemplate()) {
//				String refVarName = ref.getResourceName();
//				if (!referredSet.contains(ref)) {
//					referredSet.add(ref);
//					Expression refGetter = langSpec.getPullAccessor().getCurrentStateAccessorFor(ref, ((ResourceNode) dOut.getSource()).getIdentifierTemplate());
//					String[] sideEffects = new String[] {""};
//					String refExp = refGetter.toImplementation(sideEffects);
//					String refTypeName = ref.getResourceStateType().getInterfaceTypeName();
//					resourceUpdateMethod.addFirstStatement(sideEffects[0] + langSpec.getVariableDeclaration(refTypeName, refVarName) + langSpec.getAssignment() + refExp + langSpec.getStatementDelimiter());
//				}
//				params.add(refVarName);
//			}
//		}
		if (calleeMethod.getReturnType() == null || langSpec.isVoidType(calleeMethod.getReturnType()) || returnResources == null) {
			resourceUpdateMethod.addStatement(langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNodeName), 
																				calleeMethod.getName(), 
																				params) + langSpec.getStatementDelimiter());	// this.dst.updateSrc(value, refParams);
			return null;
		} else {
			String targetVarName = null;
			if (returnResources.size() == 1) {
				ResourceNode targetNode = returnResources.get(0);
				targetVarName = targetNode.getIdentifierTemplate().getResourceName();
				resourceUpdateMethod.addStatement(
						langSpec.getVariableDeclaration(calleeMethod.getReturnType().getInterfaceTypeName(), targetVarName) 
						+ langSpec.getAssignment() 
						+ langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNodeName), 
														calleeMethod.getName(), 
														params) + langSpec.getStatementDelimiter());	// ResType res = this.dst.updateSrc(value, refParams);
			} else {
				targetVarName = getMultipleResourcesVarName(returnResources, langSpec);
				VariableDeclaration targetVar = langSpec.newVariableDeclaration(calleeMethod.getReturnType(), targetVarName);
				List<VariableDeclaration> vars = new ArrayList<>();
				for (ResourceNode rn: returnResources) {
					IdentifierTemplate rId = rn.getIdentifierTemplate();
					vars.add(langSpec.newVariableDeclaration(rId.getResourceStateType(), rId.getResourceName()));
				}
				resourceUpdateMethod.addStatement(
						langSpec.getDecomposedTuple(
								langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNodeName), calleeMethod.getName(), params), 
								targetVar, 		// ResType res = this.dst.updateSrc(value, refParams);
								vars));			// Type1 res1 = res.getKey();	Type2 res2 = res.getValue();
			}
			return targetVarName;
		}
	}
	
	private String addInvocationInMediatorUpdate(MethodDeclaration resourceUpdateMethod, MethodDeclaration calleeMethod, String dstNodeName, List<ResourceNode> returnResources, ILanguageSpecific langSpec) {
		List<String> params = new ArrayList<>();
		if (calleeMethod.getParameters() != null) {
			for (VariableDeclaration v: calleeMethod.getParameters()) {
				params.add(v.getName());
			}
		}
		if (calleeMethod.getReturnType() == null || langSpec.isVoidType(calleeMethod.getReturnType()) || returnResources == null ) {
			resourceUpdateMethod.addStatement(langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNodeName), 
																				calleeMethod.getName(), 
																				params) + langSpec.getStatementDelimiter());	// this.dst.updateSrc(value, refParams);
			return null;
		} else {
			String targetVarName = null;
			if (returnResources.size() == 1) {
				ResourceNode targetNode = returnResources.get(0);
				targetVarName = targetNode.getIdentifierTemplate().getResourceName();
				resourceUpdateMethod.addStatement(
						langSpec.getVariableDeclaration(calleeMethod.getReturnType().getInterfaceTypeName(), targetVarName) 
						+ langSpec.getAssignment() 
						+ langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNodeName), 
														calleeMethod.getName(), 
														params) + langSpec.getStatementDelimiter());	// ResType res = this.dst.updateSrc(value, refParams);
			} else {
				targetVarName = getMultipleResourcesVarName(returnResources, langSpec);
				VariableDeclaration targetVar = langSpec.newVariableDeclaration(calleeMethod.getReturnType(), targetVarName);
				List<VariableDeclaration> vars = new ArrayList<>();
				for (ResourceNode rn: returnResources) {
					IdentifierTemplate rId = rn.getIdentifierTemplate();
					vars.add(langSpec.newVariableDeclaration(rId.getResourceStateType(), rId.getResourceName()));
				}
				resourceUpdateMethod.addStatement(
						langSpec.getDecomposedTuple(
								langSpec.getMethodInvocation(langSpec.getFieldAccessor(dstNodeName), calleeMethod.getName(), params), 
								targetVar, 		// ResType res = this.dst.updateSrc(value, refParams);
								vars));			// Type1 res1 = res.getKey();	Type2 res2 = res.getValue();
			}
			return targetVarName;
		}
	}
	
	private String getMultipleResourcesVarName(List<ResourceNode> resources, ILanguageSpecific langSpec) {
		String varName = null;
		for (ResourceNode rn: resources) {
			if (varName == null) {
				varName = rn.getIdentifierTemplate().getResourceName();
			} else {
				varName += langSpec.toComponentName(rn.getIdentifierTemplate().getResourceName());
			}
		}
		return varName;
	}
	
	private List<MethodDeclaration> getUpdateMethods(TypeDeclaration component) {
		List<MethodDeclaration> updates = new ArrayList<>();
		for (MethodDeclaration m: component.getMethods()) {
			if (m.getName().startsWith(updateMethodName)) {
				updates.add(m);
			}
		}
		return updates;
	}

	protected IResourceStateAccessor getPullAccessor(final String receiverName, final String getterOfResourceState) {
		return new IResourceStateAccessor() {
			@Override
			public Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				if (target.equals(from)) {
					return new Field(fieldOfResourceState,
							target.getResourceStateType() != null ? target.getResourceStateType()
									: DataConstraintModel.typeInt);
				}
				// for reference channel member
				Term getter = new Term(new Symbol(getterOfResourceState, 1, Symbol.Type.METHOD));
				getter.addChild(new Field(receiverName, target.getResourceStateType()));
				return getter;
			}

			@Override
			public Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				Term getter = new Term(new Symbol(getterOfResourceState, 1, Symbol.Type.METHOD));
				getter.addChild(new Field(receiverName, target.getResourceStateType()));
				return getter;
			}
		};
	}

	protected IResourceStateAccessor getPullAccessor(final String resourceAccessor) {
		return new IResourceStateAccessor() {
			@Override
			public Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				if (target.equals(from)) {
					return new Field(fieldOfResourceState,
							target.getResourceStateType() != null ? target.getResourceStateType()
									: DataConstraintModel.typeInt);
				}
				// for reference channel member
				Term getter = new Term(new Symbol(getterOfResourceState, 1, Symbol.Type.METHOD));
				getter.addChild(new Field(target.getResourceName(), target.getResourceStateType()));
				return getter;
			}

			@Override
			public Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
				return new Constant(resourceAccessor);
			}
		};
	}
}
