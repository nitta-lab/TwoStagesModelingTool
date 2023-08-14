package generators;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import algorithms.TypeInference;
import code.ast.CodeUtil;
import code.ast.CompilationUnit;
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
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResolvingMultipleDefinitionIsFutureWork;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataFlowGraph;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.StoreAttribute;
import models.dataFlowModel.DataTransferChannel.IResourceStateAccessor;

public class JerseyMethodBodyGenerator {
	private static String baseURL = "http://localhost:8080";

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
			Set<MethodDeclaration> chainedCalls = new HashSet<>();
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
								Expression updateExp = null;
								if (d.getChannel().getReferenceChannelMembers().size() == 0) {
									updateExp = d.getChannel().deriveUpdateExpressionOf(out, JerseyCodeGenerator.pushAccessor);
								} else {
									// if there exists one or more reference channel member.
									HashMap<ResourcePath, IResourceStateAccessor> inputResourceToStateAccessor = new HashMap<>();
									for (Edge eIn: dst.getInEdges()) {
										DataFlowEdge dIn = (DataFlowEdge) eIn;
										inputResourceToStateAccessor.put(((ResourceNode) dIn.getSource()).getResource(), JerseyCodeGenerator.pushAccessor);
									}
									for (ChannelMember c: d.getChannel().getReferenceChannelMembers()) {
										inputResourceToStateAccessor.put(c.getResource(), JerseyCodeGenerator.pullAccessor);
									}
									updateExp = d.getChannel().deriveUpdateExpressionOf(out, JerseyCodeGenerator.pushAccessor, inputResourceToStateAccessor);
								}
								String[] sideEffects = new String[] {""};
								String curState = updateExp.toImplementation(sideEffects);
								String updateStatement;
								if (updateExp instanceof Term && ((Term) updateExp).getSymbol().isImplWithSideEffect()) {
									updateStatement = sideEffects[0];								
								} else {
									updateStatement = sideEffects[0] + "this.value = " + curState + ";";
								}
								if (update.getBody() == null || !update.getBody().getStatements().contains(updateStatement)) {
									// add an update statement of the state of dst side resource.
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
							// to convert a json param to a tuple, pair or map object.
							for (VariableDeclaration param: update.getParameters()) {
								Type paramType = param.getType();
								String paramName = param.getName();
								String paramConverter = "";
								if (DataConstraintModel.typeList.isAncestorOf(paramType) && paramType != DataConstraintModel.typeList) {
									Type compType = TypeInference.getListComponentType(paramType);
									if (DataConstraintModel.typeTuple.isAncestorOf(compType)) {
										param.setType(DataConstraintModel.typeListStr);
										param.setName(paramName + "_json");
										paramConverter += paramType.getInterfaceTypeName() + " " + paramName + " = new " + paramType.getImplementationTypeName() + "();\n";
										paramConverter += "for (String str: " + param.getName() + ") {\n";
										String mapTypeName = convertFromEntryToMapType(compType);
										paramConverter += "\t" + mapTypeName + " i = new ObjectMapper().readValue(str, HashMap.class);\n";
										paramConverter += "\t" + paramName + ".add(" + getCodeForConversionFromMapToTuple(compType, "i") + ");\n";
										paramConverter += "}";
										update.addThrow("JsonProcessingException");
									} else if (DataConstraintModel.typePair.isAncestorOf(compType)) {
										param.setType(DataConstraintModel.typeListStr);
										param.setName(paramName + "_json");
										paramConverter += paramType.getInterfaceTypeName() + " " + paramName + " = new " + paramType.getImplementationTypeName() + "();\n";
										paramConverter += "for (String str: " + param.getName() + ") {\n";
										String mapTypeName = convertFromEntryToMapType(compType);
										paramConverter += "\t" + mapTypeName + " i = new ObjectMapper().readValue(str, HashMap.class);\n";
										paramConverter += "\t" + paramName + ".add(" + getCodeForConversionFromMapToPair(compType, "i") + ");\n";
										paramConverter += "}";
										update.addThrow("JsonProcessingException");
									} else if (DataConstraintModel.typeMap.isAncestorOf(compType)) {
										param.setType(DataConstraintModel.typeListStr);
										// To do.
									}
								} else if (DataConstraintModel.typeTuple.isAncestorOf(paramType)) {
									param.setType(DataConstraintModel.typeString);
									param.setName(paramName + "_json");
									paramConverter += paramType.getInterfaceTypeName() + " " + paramName + ";\n";
									paramConverter += "{\n";
									String mapTypeName = convertFromEntryToMapType(paramType);
									paramConverter += "\t" + mapTypeName + " i = new ObjectMapper().readValue(" + paramName + "_json" + ", HashMap.class);\n";
									paramConverter += "\t" + paramName + " = " + getCodeForConversionFromMapToTuple(paramType, "i") + ";\n";
									paramConverter += "}";
									update.addThrow("JsonProcessingException");
								} else if (DataConstraintModel.typePair.isAncestorOf(paramType)) {
									param.setType(DataConstraintModel.typeString);
									param.setName(paramName + "_json");
									paramConverter += paramType.getInterfaceTypeName() + " " + paramName + ";\n";
									paramConverter += "{\n";
									String mapTypeName = convertFromEntryToMapType(paramType);
									paramConverter += "\t" + mapTypeName + " i = new ObjectMapper().readValue(" + paramName + "_json" + ", HashMap.class);\n";
									paramConverter += "\t" + paramName + " = " + getCodeForConversionFromMapToPair(paramType, "i") + ";\n";
									paramConverter += "}";
									update.addThrow("JsonProcessingException");
								} else if (DataConstraintModel.typeMap.isAncestorOf(paramType)) {
									param.setType(DataConstraintModel.typeString);
									param.setName(paramName + "_json");
									paramConverter += paramType.getInterfaceTypeName() + " " + paramName + " = " + "new " + paramType.getImplementationTypeName() + "();\n";
									paramConverter += "{\n";
									String mapTypeName = convertFromEntryToMapType(paramType);
									paramConverter += "\t" + mapTypeName + " i = new ObjectMapper().readValue(" + paramName + "_json" + ", HashMap.class);\n";
									paramConverter += "\t" + getCodeForConversionFromMapToMap(paramType, "i", paramName) + "\n";
									paramConverter += "}";
									update.addThrow("JsonProcessingException");
								}
								if (paramConverter.length() > 0) update.addFirstStatement(paramConverter);
							}
							MethodDeclaration getter = getGetterMethod(dstType);
							if (((StoreAttribute) dst.getAttribute()).isStored()) {
								// returns the state stored in a field.
								if (getter.getBody() == null || getter.getBody().getStatements().size() == 0) {
									getter.addStatement("return value;");
								}
							}
							// src side (for a chain of update method invocations)
							String httpMethod = null;
							if (out.getStateTransition().isRightUnary()) {
								httpMethod = "put";									
							} else {
								httpMethod = "post";
							}
							for (MethodDeclaration srcUpdate: getUpdateMethods(srcType)) {
								if (srcUpdate != null) {
									List<Map.Entry<Type, Map.Entry<String, String>>> params = new ArrayList<>();
									Set<ResourcePath> referredSet = referredResources.get(srcUpdate);
									if (d.getChannel().getReferenceChannelMembers().size() > 0) {
										for (ChannelMember rc: d.getChannel().getReferenceChannelMembers()) {
											// For each reference channel member, get the current state of the reference side resource by pull data transfer.
											ResourcePath ref = rc.getResource();
											if (referredSet == null) {
												referredSet = new HashSet<>();
												referredResources.put(srcUpdate, referredSet);
											}
											if (!ref.equals(dst.getResource())) {
												String refResourceName = ref.getResourceName();
												Type refResourceType = ref.getResourceStateType();
												if (!referredSet.contains(ref)) {
													referredSet.add(ref);
													generatePullDataTransfer(srcUpdate, refResourceName, refResourceType);
												}
												// Value of a reference side resource.
												params.add(new AbstractMap.SimpleEntry<>(refResourceType, new AbstractMap.SimpleEntry<>(refResourceName, refResourceName)));
											}
										}
									}
									String srcResName = null;
									if (dst.getIndegree() > 1) {
										srcResName = srcResourceName;
									}
									if (!chainedCalls.contains(srcUpdate)) {
										// The first call to an update method in this method
										// Value of the source side (input side) resource.
										params.add(0, new AbstractMap.SimpleEntry<>(src.getResource().getResourceStateType(), new AbstractMap.SimpleEntry<>(srcResourceName, "this.value")));
										srcUpdate.addStatement(getHttpMethodParamsStatement(srcType.getTypeName(), params, true));
										srcUpdate.addStatement("String result = " + getHttpMethodCallStatement(baseURL, dstResourceName, srcResName, httpMethod));
										chainedCalls.add(srcUpdate);
									} else {
										// After the second time of call to update methods in this method
										// Value of the source side (input side) resource.
										params.add(0, new AbstractMap.SimpleEntry<>(src.getResource().getResourceStateType(), new AbstractMap.SimpleEntry<>(srcResourceName, "this.value")));
										srcUpdate.addStatement(getHttpMethodParamsStatement(srcType.getTypeName(), params, false));
										srcUpdate.addStatement("result = " + getHttpMethodCallStatement(baseURL, dstResourceName, srcResName, httpMethod));
									}
									srcUpdate.addThrow("JsonProcessingException");
								}
							}
							for (MethodDeclaration srcInput: getInputMethods(srcType, src, model)) {
								List<Map.Entry<Type, Map.Entry<String, String>>> params = new ArrayList<>();
								Set<ResourcePath> referredSet = referredResources.get(srcInput);
								for (ChannelMember rc: d.getChannel().getReferenceChannelMembers()) {
									// For each reference channel member, get the current state of the reference side resource by pull data transfer.
									ResourcePath ref = rc.getResource();
									if (referredSet == null) {
										referredSet = new HashSet<>();
										referredResources.put(srcInput, referredSet);
									}
									if (!ref.equals(dst.getResource())) {
										String refResourceName = ref.getResourceName();
										Type refResourceType = ref.getResourceStateType();
										if (!referredSet.contains(ref)) {
											referredSet.add(ref);
											generatePullDataTransfer(srcInput, refResourceName, refResourceType);
										}
										// Value of a reference side resource.
										params.add(new AbstractMap.SimpleEntry<>(refResourceType, new AbstractMap.SimpleEntry<>(refResourceName, refResourceName)));
									}									
								}
								String srcResName = null;
								if (dst.getIndegree() > 1) {
									srcResName = srcResourceName;
								}
								if (!chainedCalls.contains(srcInput)) {
									// First call to an update method in this method
									// Value of the source side (input side) resource.
									params.add(0, new AbstractMap.SimpleEntry<>(src.getResource().getResourceStateType(), new AbstractMap.SimpleEntry<>(srcResourceName, "this.value")));
									srcInput.addStatement(getHttpMethodParamsStatement(srcType.getTypeName(), params, true));
									srcInput.addStatement("String result = " + getHttpMethodCallStatement(baseURL, dstResourceName, srcResName, httpMethod));
									chainedCalls.add(srcInput);
								} else {
									// After the second time of call to update methods in this method
									// Value of the source side (input side) resource.
									params.add(0, new AbstractMap.SimpleEntry<>(src.getResource().getResourceStateType(), new AbstractMap.SimpleEntry<>(srcResourceName, "this.value")));
									srcInput.addStatement(getHttpMethodParamsStatement(srcType.getTypeName(), params, false));
									srcInput.addStatement("result = " + getHttpMethodCallStatement(baseURL, dstResourceName, srcResName, httpMethod));
								}
								srcInput.addThrow("JsonProcessingException");
							}
						} else {
							// for pull (or push/pull) data transfer
							MethodDeclaration getter = getGetterMethod(dstType);
							if (getter.getBody() == null || getter.getBody().getStatements().size() == 0) {
								// generate a return statement.
								String[] sideEffects = new String[] {""};
								String curState = d.getChannel().deriveUpdateExpressionOf(out, JerseyCodeGenerator.pullAccessor).toImplementation(sideEffects);		// no pull data transfer is included.
								getter.addStatement(sideEffects[0] + "return " + curState + ";");
								// For each reference channel member, get the current state of the reference side resource by pull data transfer.
								for (ChannelMember c: d.getChannel().getReferenceChannelMembers()) {
									String refResourceName = c.getResource().getResourceName();
									Type refResourceType = c.getResource().getResourceStateType();
									generatePullDataTransfer(getter, refResourceName, refResourceType);
								}
							}
							// get src side resource state by pull data transfer.
							Type srcResourceType = src.getResource().getResourceStateType();
							generatePullDataTransfer(getter, srcResourceName, srcResourceType);
						} 
					}
				}
			}
			// for source nodes
			for (Node n: graph.getNodes()) {
				ResourceNode resource = (ResourceNode) n;
				String resourceName = resource.getResource().getResourceName();
				TypeDeclaration type = typeMap.get(resourceName);
				if (type != null) {
					// getter method
					MethodDeclaration getter = getGetterMethod(type);
					if (getter.getBody() == null || getter.getBody().getStatements().size() == 0) {
						getter.addStatement("return value;");
					}
					// methods for input events
					Map<DataTransferChannel, Set<ChannelMember>> ioChannelsAndMembers = getIOChannelsAndMembers(resource, model);
					for (Map.Entry<DataTransferChannel, Set<ChannelMember>> entry: ioChannelsAndMembers.entrySet()) {
						Set<ChannelMember> outs = entry.getValue();
						for (ChannelMember out: outs) {
							MethodDeclaration input = getInputMethod(type, out);
							if (input != null) {
								Expression updateExp = entry.getKey().deriveUpdateExpressionOf(out, JerseyCodeGenerator.pushAccessor);
								String[] sideEffects = new String[] {""};
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

	private static void generatePullDataTransfer(MethodDeclaration methodBody, String fromResourceName, Type fromResourceType) {
		String varName = new String(fromResourceName);
		String respTypeName = fromResourceType.getInterfaceTypeName();
		String respImplTypeName = fromResourceType.getImplementationTypeName();
		String respConverter = "";
		if (DataConstraintModel.typeList.isAncestorOf(fromResourceType) && fromResourceType != DataConstraintModel.typeList) {
			Type compType = TypeInference.getListComponentType(fromResourceType);
			if (DataConstraintModel.typeTuple.isAncestorOf(compType)) {
				varName += "_json";
				String mapTypeName = convertFromEntryToMapType(compType);
				respTypeName = "List<" + mapTypeName + ">";
				respConverter += fromResourceType.getInterfaceTypeName() + " " + fromResourceName + " = new " + fromResourceType.getImplementationTypeName() + "();\n";
				respConverter += "for (" + mapTypeName + " i: " + varName + ") {\n";
				respConverter += "\t" + fromResourceName + ".add(" + getCodeForConversionFromMapToTuple(compType, "i") + ");\n";
				respConverter += "}";
				methodBody.addThrow("JsonProcessingException");
			} else if (DataConstraintModel.typeMap.isAncestorOf(compType)) {
				// To do.
			}
		} else if (DataConstraintModel.typeTuple.isAncestorOf(fromResourceType)) {
			varName += "_json";
			respTypeName = convertFromEntryToMapType(fromResourceType);
			respConverter += fromResourceType.getInterfaceTypeName() + " " + fromResourceName + " = " + getCodeForConversionFromMapToTuple(fromResourceType, varName) + ";";
			respImplTypeName = "HashMap";
		} else if (DataConstraintModel.typePair.isAncestorOf(fromResourceType)) {
			varName += "_json";
			respTypeName = convertFromEntryToMapType(fromResourceType);
			respConverter += fromResourceType.getInterfaceTypeName() + " " + fromResourceName + " = " + getCodeForConversionFromMapToPair(fromResourceType, varName) + ";";
			respImplTypeName = "HashMap";
		} else if (DataConstraintModel.typeMap.isAncestorOf(fromResourceType)) {
			varName += "_json";
			respTypeName = convertFromEntryToMapType(fromResourceType);
			respConverter += fromResourceType.getInterfaceTypeName() + " " + fromResourceName + " = new " + fromResourceType.getImplementationTypeName() + "();\n";
			respConverter += getCodeForConversionFromMapToMap(fromResourceType, varName, fromResourceName);
			respImplTypeName = "HashMap";
		}
		if (respConverter.length() > 0) {
			methodBody.addFirstStatement(respConverter);
		}
		methodBody.addFirstStatement(respTypeName + " " + varName + " = " + getHttpMethodCallStatementWithResponse(baseURL, fromResourceName, "get", respImplTypeName));
	}

	private static String convertFromEntryToMapType(Type type) {
		String mapTypeName = null;
		if (DataConstraintModel.typePair.isAncestorOf(type)) {
			Type compType = TypeInference.getPairComponentType(type);
			String wrapperType = DataConstraintModel.getWrapperType(compType);
			if (wrapperType != null) {
				mapTypeName = "Map<String, " + wrapperType + ">";
			} else {
				mapTypeName = "Map<String, " + compType.getInterfaceTypeName() + ">";
			}
		} else if (DataConstraintModel.typeMap.isAncestorOf(type)) {
			List<Type> compTypes = TypeInference.getMapComponentTypes(type);
			String wrapperType = DataConstraintModel.getWrapperType(compTypes.get(1));
			if (wrapperType != null) {
				mapTypeName = "Map<String, " + wrapperType + ">";
			} else {
				mapTypeName = "Map<String, " + compTypes.get(1).getInterfaceTypeName() + ">";
			}
		} else {
			mapTypeName = type.getInterfaceTypeName();
			mapTypeName = mapTypeName.replace("Map.Entry", "Map");
			for (int idx = mapTypeName.indexOf("<", 0); idx >= 0; idx = mapTypeName.indexOf("<", idx + 1)) {
				int to = mapTypeName.indexOf(",", idx);
				if (to > idx) {
					mapTypeName = mapTypeName.substring(0, idx + 1) + "String" + mapTypeName.substring(to);		// All elements except for the last one have the string type.
				}
			}
		}
		return mapTypeName;
	}

	private static String getCodeForConversionFromMapToTuple(Type tupleType, String mapVar) {
		String decoded = "$x";
		List<Type> elementsTypes = TypeInference.getTupleComponentTypes(tupleType);
		String elementBase = mapVar;
		for (Type elmType: elementsTypes.subList(0, elementsTypes.size() - 1)) {
			elementBase += ".entrySet().iterator().next()";
			if (elmType == DataConstraintModel.typeBoolean
					|| elmType == DataConstraintModel.typeInt
					|| elmType == DataConstraintModel.typeLong
					|| elmType == DataConstraintModel.typeFloat
					|| elmType == DataConstraintModel.typeDouble) {
				String elmVal = CodeUtil.getToValueExp(elmType.getImplementationTypeName(), elementBase + ".getKey()");
				decoded = decoded.replace("$x", "new AbstractMap.SimpleEntry<>(" + elmVal + ", $x)");
			} else if (elmType == DataConstraintModel.typeString) {
				decoded = decoded.replace("$x", "new AbstractMap.SimpleEntry<>(" + elementBase + ".getKey(), $x)");
			} else {
				// To do.
			}
			elementBase += ".getValue()";
		}
		decoded = decoded.replace("$x", elementBase);
		return decoded;
	}

	private static String getCodeForConversionFromMapToPair(Type pairType, String mapVar) {
		String decoded = "$x";
		decoded = decoded.replace("$x", "new Pair<>(" + mapVar + ".get(\"left\"), $x)");
		decoded = decoded.replace("$x", mapVar + ".get(\"right\")");
		return decoded;
	}

	private static String getCodeForConversionFromMapToMap(Type mapType, String mapVal, String mapVar) {
		List<Type> elementsTypes = TypeInference.getMapComponentTypes(mapType);
		Type keyType = elementsTypes.get(0);
		Type valType = elementsTypes.get(1);
		String keyVal = null;
		String decoded = "";
		if (keyType == DataConstraintModel.typeBoolean
				|| keyType == DataConstraintModel.typeInt
				|| keyType == DataConstraintModel.typeLong
				|| keyType == DataConstraintModel.typeFloat
				|| keyType == DataConstraintModel.typeDouble) {
			decoded += "for (String k: " + mapVal + ".keySet()) {\n";
			decoded += "\t" + mapVar + ".put(";
			keyVal = CodeUtil.getToValueExp(keyType.getImplementationTypeName(), "k");
			decoded += keyVal + ", " + mapVal + ".get(" + keyVal + ")" + ");\n";
			decoded += "}";
		} else if (keyType == DataConstraintModel.typeString) {
			decoded += mapVar + " = " + mapVal + ";";
		}
		return decoded;
	}

	private static String getHttpMethodParamsStatement(String callerResourceName, List<Map.Entry<Type, Map.Entry<String, String>>> params, boolean isFirstCall) {
		String statements = "";
		if (isFirstCall) {
			statements += "Form ";
		}
		statements += "form = new Form();\n";
		for (Map.Entry<Type, Map.Entry<String, String>> param: params) {
			Type paramType = param.getKey();
			String paramName = param.getValue().getKey();
			String value = param.getValue().getValue();
			if (DataConstraintModel.typeList.isAncestorOf(paramType)) {
				Type compType = TypeInference.getListComponentType(paramType);
				String wrapperType = DataConstraintModel.getWrapperType(compType);
				if (wrapperType == null) {
					statements += "for (" + compType.getInterfaceTypeName() + " i: " + value + ") {\n";
				} else {
					statements += "for (" + wrapperType + " i: " + value + ") {\n";
				}
				if (DataConstraintModel.typeTuple.isAncestorOf(compType) || DataConstraintModel.typePair.isAncestorOf(paramType) || DataConstraintModel.typeList.isAncestorOf(compType) || DataConstraintModel.typeMap.isAncestorOf(paramType)) {
					statements += "\tform.param(\"" + paramName + "\", new ObjectMapper().writeValueAsString(i));\n";		// typeTuple: {"1.0":2.0},  typePair: {"left": 1.0, "right":2.0}
				} else {
					statements += "\tform.param(\"" + paramName + "\", i.toString());\n";
				}
				statements += "}\n";
//				return "Entity<String> entity = Entity.entity(" + paramName + ".toString(), MediaType.APPLICATION_JSON);";
			} else if (DataConstraintModel.typeTuple.isAncestorOf(paramType) || DataConstraintModel.typePair.isAncestorOf(paramType) || DataConstraintModel.typeMap.isAncestorOf(paramType)) {
				// typeTuple: {"1.0":2.0},  typePair: {"left": 1.0, "right":2.0}
				statements += "form.param(\"" + paramName + "\", new ObjectMapper().writeValueAsString(" + value + "));\n";			
			} else {
				statements += "form.param(\"" + paramName + "\", " + CodeUtil.getToStringExp(paramType.getImplementationTypeName(), value) + ");\n";
			}
		}
		if (isFirstCall) {
			statements += "Entity<Form> ";
		}
		statements += "entity = Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED);";
		return statements;
	}

	private static String getHttpMethodCallStatement(String baseURL, String resourceName, String srcResName, String httpMethod) {
		if (srcResName == null) {
			return "client.target(\"" + baseURL + "\").path(\"/" + resourceName + "\").request()." + httpMethod + "(entity, String.class);";
		} else {
			 // For each source resource, a child resource is defined in the destination resource so that its state can be updated separately.
			return "client.target(\"" + baseURL + "\").path(\"/" + resourceName + "/" + srcResName + "\").request()." + httpMethod + "(entity, String.class);";
		}
	}
	
	private static String getHttpMethodCallStatementWithResponse(String baseURL, String resourceName, String httpMethod, String respImplName) {
		String responseShortTypeName = respImplName;
		if (respImplName.contains("<")) {
			responseShortTypeName = respImplName.substring(0, respImplName.indexOf("<"));
		}
		return "client.target(\"" + baseURL + "\").path(\"/" + resourceName + "\").request()." + httpMethod + "(" + responseShortTypeName + ".class);";	
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
