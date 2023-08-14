package generators;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import code.ast.CompilationUnit;
import code.ast.MethodDeclaration;
import code.ast.TypeDeclaration;
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
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.DataTransferChannel.IResourceStateAccessor;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResolvingMultipleDefinitionIsFutureWork;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataFlowGraph;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.StoreAttribute;

public class JavaMethodBodyGenerator {
	public static ArrayList<CompilationUnit> doGenerate(DataFlowGraph graph, DataTransferModel model, ArrayList<CompilationUnit> codes) {
		// Create a map from type names (lower case) to their types.
		Map<String, TypeDeclaration> typeMap = new HashMap<>();
		for (CompilationUnit code: codes) {
			for (TypeDeclaration type: code.types()) {
				typeMap.put(type.getTypeName().substring(0,1).toLowerCase() + type.getTypeName().substring(1), type);
			}
		}
		
		// Generate the body of each update or getter method.
		try {
			Map<MethodDeclaration, Set<ResourcePath>> referredResources = new HashMap<>(); 
			for (Edge e: graph.getEdges()) {
				DataFlowEdge d = (DataFlowEdge) e;
				PushPullAttribute pushPull = (PushPullAttribute) d.getAttribute();
				ResourceNode src = (ResourceNode) d.getSource();
				ResourceNode dst = (ResourceNode) d.getDestination();
				String srcResourceName = src.getResource().getResourceName();
				String dstResourceName = dst.getResource().getResourceName();
				TypeDeclaration srcType = typeMap.get(srcResourceName);
				TypeDeclaration dstType = typeMap.get(dstResourceName);
				for (ChannelMember out: d.getChannel().getOutputChannelMembers()) {
					if (out.getResource().equals(dst.getResource())) {
						if (pushPull.getOptions().get(0) == PushPullValue.PUSH && srcType != null) {
							// for push data transfer
							MethodDeclaration update = getUpdateMethod(dstType, srcType);
							if (((StoreAttribute) dst.getAttribute()).isStored()) {
								// update stored state of dst side resource (when every incoming edge is in push style)
								Expression updateExp = d.getChannel().deriveUpdateExpressionOf(out, JavaCodeGenerator.pushAccessor);									
								String[] sideEffects = new String[] {""};
								String curState = updateExp.toImplementation(sideEffects);
								String updateStatement;
								if (updateExp instanceof Term && ((Term) updateExp).getSymbol().isImplWithSideEffect()) {
									updateStatement = sideEffects[0];
								} else {
									updateStatement = sideEffects[0] + "value = " + curState + ";";
								}
								if (update.getBody() == null || !update.getBody().getStatements().contains(updateStatement)) {
									update.addFirstStatement(updateStatement);
								}
							}
							if (dst.getIndegree() > 1) {
								// update a cash of src side resource (when incoming edges are multiple)
								String cashStatement = "this." + srcResourceName + " = " + srcResourceName + ";";
								if (update.getBody() == null || !update.getBody().getStatements().contains(cashStatement)) {
									update.addFirstStatement(cashStatement);
								}								
							}
							MethodDeclaration getter = getGetterMethod(dstType);
							if (((StoreAttribute) dst.getAttribute()).isStored()) {
								// returns the current state stored in a field.
								if (getter.getBody() == null || getter.getBody().getStatements().size() == 0) {
									Type resourceType = dst.getResource().getResourceStateType();
									if (model.isPrimitiveType(resourceType)) {
										getter.addStatement("return value;");
									} else {
										// copy the current state to be returned as a 'value'
										String implTypeName = resourceType.getImplementationTypeName();
//										String interfaceTypeName = resourceType.getInterfaceTypeName();
//										String concreteTypeName;
//										if (interfaceTypeName.contains("<")) {
//											String typeName = implTypeName.substring(0, implTypeName.indexOf("<"));
////											String generics = interfaceTypeName.substring(interfaceTypeName.indexOf("<") + 1, interfaceTypeName.lastIndexOf(">"));
//											concreteTypeName = typeName + "<>";
//										} else {
//											concreteTypeName = implTypeName;
//										}
										getter.addStatement("return new " + implTypeName + "(value);");
									}
								}
							}
							// src side (for a chain of update method invocations)
							for (MethodDeclaration srcUpdate: getUpdateMethods(srcType)) {
								String refParams = "";
								Set<ResourcePath> referredSet = referredResources.get(srcUpdate);
								for (ChannelMember rc: d.getChannel().getReferenceChannelMembers()) {
									// to get the value of reference member.
									ResourcePath ref = rc.getResource();
									if (referredSet == null) {
										referredSet = new HashSet<>();
										referredResources.put(srcUpdate, referredSet);
									}
									if (!ref.equals(dst.getResource())) {
										String refVarName = ref.getResourceName();
										if (!referredSet.contains(ref)) {
											referredSet.add(ref);
											Expression refGetter = JavaCodeGenerator.pullAccessor.getCurrentStateAccessorFor(ref, src.getResource());
											String[] sideEffects = new String[] {""};
											String refExp = refGetter.toImplementation(sideEffects);
											String refTypeName = ref.getResourceStateType().getInterfaceTypeName();
											srcUpdate.addFirstStatement(sideEffects[0] + refTypeName + " " + refVarName + " = " + refExp + ";");
										}
										refParams += ", " + refVarName;
									}
								}
								srcUpdate.addStatement("this." + dstResourceName + ".update" + srcType.getTypeName() + "(value" + refParams + ");");
							}
							for (MethodDeclaration srcInput: getInputMethods(srcType, src, model)) {
								String refParams = "";
								Set<ResourcePath> referredSet = referredResources.get(srcInput);
								for (ChannelMember rc: d.getChannel().getReferenceChannelMembers()) {
									// to get the value of reference member.
									ResourcePath ref = rc.getResource();
									if (referredSet == null) {
										referredSet = new HashSet<>();
										referredResources.put(srcInput, referredSet);
									}
									if (!ref.equals(dst.getResource())) {
										String refVarName = ref.getResourceName();
										if (!referredSet.contains(ref)) {
											referredSet.add(ref);
											Expression refGetter = JavaCodeGenerator.pullAccessor.getCurrentStateAccessorFor(ref, src.getResource());
											String[] sideEffects = new String[] {""};
											String refExp = refGetter.toImplementation(sideEffects);
											String refTypeName = ref.getResourceStateType().getInterfaceTypeName();
											srcInput.addFirstStatement(sideEffects[0] + refTypeName + " " + refVarName + " = " + refExp + ";");
										}
										refParams += ", " + refVarName;
									}
								}
								srcInput.addStatement("this." + dstResourceName + ".update" + srcType.getTypeName() + "(value" + refParams + ");");
							}
						} else {
							// for pull (or push/pull) data transfer
							MethodDeclaration getter = getGetterMethod(dstType);
							if (getter.getBody() == null || getter.getBody().getStatements().size() == 0) {
								boolean isContainedPush = false;
								HashMap<ResourcePath, IResourceStateAccessor> inputResourceToStateAccessor = new HashMap<>();
								for (Edge eIn: dst.getInEdges()) {
									DataFlowEdge dIn = (DataFlowEdge) eIn;
									if (((PushPullAttribute) dIn.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
										isContainedPush = true;
										inputResourceToStateAccessor.put(((ResourceNode) dIn.getSource()).getResource(), JavaCodeGenerator.pushAccessor);
									} else {
										inputResourceToStateAccessor.put(((ResourceNode) dIn.getSource()).getResource(), JavaCodeGenerator.pullAccessor);
									}
								}
								// for reference channel members
								for (ChannelMember c: d.getChannel().getReferenceChannelMembers()) {
									inputResourceToStateAccessor.put(c.getResource(), JavaCodeGenerator.pullAccessor);			// by pull data transfer
								}
								String[] sideEffects = new String[] {""};
								// generate a return statement.
								if (!isContainedPush) {
									// All incoming edges are in PULL style.
									String curState = d.getChannel().deriveUpdateExpressionOf(out, JavaCodeGenerator.pullAccessor).toImplementation(sideEffects);
									getter.addStatement(sideEffects[0] + "return " + curState + ";");
								} else {
									// At least one incoming edge is in PUSH style.
									String curState = d.getChannel().deriveUpdateExpressionOf(out, JavaCodeGenerator.pullAccessor, inputResourceToStateAccessor).toImplementation(sideEffects);
									getter.addStatement(sideEffects[0] + "return " + curState + ";");
								}
							}
						} 
					}
				}
			}
			// for source nodes
			String mainTypeName = JavaCodeGenerator.mainTypeName.substring(0,1).toLowerCase() + JavaCodeGenerator.mainTypeName.substring(1);
			TypeDeclaration mainType = typeMap.get(mainTypeName);
			for (Node n: graph.getNodes()) {
				ResourceNode resource = (ResourceNode) n;
				String resourceName = resource.getResource().getResourceName();
				TypeDeclaration type = typeMap.get(resourceName);
				if (type != null) {
					// getter method
					MethodDeclaration getter = getGetterMethod(type);
					if (getter.getBody() == null || getter.getBody().getStatements().size() == 0) {
						Type resourceType = resource.getResource().getResourceStateType();
						if (model.isPrimitiveType(resourceType)) {
							getter.addStatement("return value;");							
						} else {
							// copy the current state to be returned as a 'value'
							String implTypeName = resourceType.getImplementationTypeName();
//							String interfaceTypeName = resourceType.getInterfaceTypeName();
//							String concreteTypeName;
//							if (interfaceTypeName.contains("<")) {
//								String typeName = implTypeName.substring(0, implTypeName.indexOf("<"));
//								String generics = interfaceTypeName.substring(interfaceTypeName.indexOf("<") + 1, interfaceTypeName.lastIndexOf(">"));
//								concreteTypeName = typeName + "<" + generics + ">";
//							} else {
//								concreteTypeName = implTypeName;
//							}
							getter.addStatement("return new " + implTypeName + "(value);");
						}
					}
					// methods for input events
					Map<DataTransferChannel, Set<ChannelMember>> ioChannelsAndMembers = getIOChannelsAndMembers(resource, model);
					for (Map.Entry<DataTransferChannel, Set<ChannelMember>> entry: ioChannelsAndMembers.entrySet()) {
						Set<ChannelMember> outs = entry.getValue();
						for (ChannelMember out: outs) {
							MethodDeclaration input = getInputMethod(type, out);
							if (input != null) {
								String[] sideEffects = new String[] {""};
								Expression updateExp = entry.getKey().deriveUpdateExpressionOf(out, JavaCodeGenerator.pushAccessor);
								String newState = updateExp.toImplementation(sideEffects);
								String updateStatement;
								if (updateExp instanceof Term && ((Term) updateExp).getSymbol().isImplWithSideEffect()) {
									updateStatement = sideEffects[0];	
								} else {
									updateStatement = sideEffects[0] + "this.value = " + newState + ";";
								}
								if (input.getBody() == null || !input.getBody().getStatements().contains(updateStatement)) {
									input.addFirstStatement(updateStatement);
								}
								if (mainType != null) {
									MethodDeclaration mainInput = getMethod(mainType, input.getName());
									if (mainInput != null) {
										String args = "";
										String delimitar = "";
										if (out.getStateTransition().getMessageExpression() instanceof Term) {
											Term message = (Term) out.getStateTransition().getMessageExpression();
											for (Variable var: message.getVariables().values()) {
												args += delimitar + var.getName();
												delimitar = ", ";
											}
										}
										mainInput.addStatement("this." + resourceName + "." + input.getName() + "(" + args + ");");
									}
								}
							}
						}
					}
				}
			}
		} catch (ParameterizedIdentifierIsFutureWork | ResolvingMultipleDefinitionIsFutureWork
				| InvalidMessage | UnificationFailed | ValueUndefined e1) {
			e1.printStackTrace();
		}
		return codes;
	}

	private static MethodDeclaration getUpdateMethod(TypeDeclaration type, TypeDeclaration from) {
		for (MethodDeclaration m: type.getMethods()) {
			if (m.getName().equals("update" + from.getTypeName())) return m;
		}
		return null;
	}

	private static List<MethodDeclaration> getUpdateMethods(TypeDeclaration type) {
		List<MethodDeclaration> updates = new ArrayList<>();
		for (MethodDeclaration m: type.getMethods()) {
			if (m.getName().startsWith("update")) {
				updates.add(m);
			}
		}
		return updates;
	}

	private static MethodDeclaration getGetterMethod(TypeDeclaration type) {
		for (MethodDeclaration m: type.getMethods()) {
			if (m.getName().startsWith("get")) return m;
		}
		return null;
	}
	
	private static Map<DataTransferChannel, Set<ChannelMember>> getIOChannelsAndMembers(ResourceNode resource, DataTransferModel model) {
		Map<DataTransferChannel, Set<ChannelMember>> ioChannelsAndMembers = new HashMap<>();
		for (Channel c: model.getIOChannels()) {
			DataTransferChannel ch = (DataTransferChannel) c;
			// I/O channel
			for (ChannelMember out: ch.getOutputChannelMembers()) {
				if (out.getResource().equals(resource.getResource())) {
					if (out.getStateTransition().getMessageExpression() instanceof Term || out.getStateTransition().getMessageExpression() instanceof Variable) {
						Set<ChannelMember> channelMembers = ioChannelsAndMembers.get(ch);
						if (channelMembers == null) {
							channelMembers = new HashSet<>();
							ioChannelsAndMembers.put(ch, channelMembers);
						}
						channelMembers.add(out);
					}
				}
			}
		}
		return ioChannelsAndMembers;
	}

	private static List<MethodDeclaration> getInputMethods(TypeDeclaration type, ResourceNode resource, DataTransferModel model) {
		List<MethodDeclaration> inputs = new ArrayList<>();
		for (Channel c: model.getIOChannels()) {
			DataTransferChannel channel = (DataTransferChannel) c;
			// I/O channel
			for (ChannelMember out: channel.getOutputChannelMembers()) {
				if (out.getResource().equals(resource.getResource())) {
					MethodDeclaration input = getInputMethod(type, out);
					inputs.add(input);
				}
			}
		}
		return inputs;
	}

	private static MethodDeclaration getInputMethod(TypeDeclaration type, ChannelMember out) {
		MethodDeclaration input = null;
		if (out.getStateTransition().getMessageExpression() instanceof Term) {
			Term message = (Term) out.getStateTransition().getMessageExpression();
			input = getMethod(type, message.getSymbol().getImplName());
		} else if (out.getStateTransition().getMessageExpression() instanceof Variable) {
			Variable message = (Variable) out.getStateTransition().getMessageExpression();
			input = getMethod(type, message.getName());
		}
		return input;
	}

	private static MethodDeclaration getMethod(TypeDeclaration type, String methodName) {
		for (MethodDeclaration m: type.getMethods()) {
			if (m.getName().equals(methodName)) return m;
		}
		return null;
	}
}
