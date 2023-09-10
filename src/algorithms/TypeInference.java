package algorithms;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.crypto.Data;

import models.Node;
import models.algebra.Constant;
import models.algebra.Expression;
import models.algebra.Position;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Type;
import models.algebra.Variable;
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.JsonType;
import models.dataConstraintModel.ResourcePath;
import models.dataConstraintModel.StateTransition;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.ResourceNode;

/**
 * Type inference for data transfer model
 * 
 * @author Nitta
 *
 */
public class TypeInference {
	static private Map<Type, Type> listTypes = new HashMap<>();
	static private Map<Type, Type> listComponentTypes = new HashMap<>();
	static private Map<List<Type>, Type> tupleTypes = new HashMap<>();
	static private Map<Type, List<Type>> tupleComponentTypes = new HashMap<>();
	static private Map<Type, Type> pairTypes = new HashMap<>();
	static private Map<Type, Type> pairComponentTypes = new HashMap<>();
	static private Map<List<Type>, Type> mapTypes = new HashMap<>();
	static private Map<Type, List<Type>> mapComponentTypes = new HashMap<>();
	static private Map<Map<String, Type>, Type> jsonTypes = new HashMap<>();
	static private Map<Type, Map<String, Type>> jsonMemberTypes = new HashMap<>();

	public static Type getListType(Type compType) {
		return listTypes.get(compType);
	}

	public static Type getListComponentType(Type listType) {
		return listComponentTypes.get(listType);
	}

	public static Collection<Type> getListTypes() {
		return listTypes.values();
	}

	public static Type getTupleType(List<Type> compTypes) {
		return tupleTypes.get(compTypes);
	}

	public static List<Type> getTupleComponentTypes(Type tupleType) {
		return tupleComponentTypes.get(tupleType);
	}

	public static Collection<Type> getTupleTypes() {
		return tupleTypes.values();
	}

	public static Type getPairType(Type compType) {
		return pairTypes.get(compType);
	}

	public static Type getPairComponentType(Type pairType) {
		return pairComponentTypes.get(pairType);
	}

	public static Type getMapType(List<Type> compTypes) {
		return mapTypes.get(compTypes);
	}

	public static List<Type> getMapComponentTypes(Type mapType) {
		return mapComponentTypes.get(mapType);
	}

	public static Type getJsonType(Map<String, Type> memberTypes) {
		return jsonTypes.get(memberTypes);
	}

	public static Map<String, Type> getJsonMemberTypes(Type jsonType) {
		return jsonMemberTypes.get(jsonType);
	}

	static public void infer(DataTransferModel model) {
		Map<ResourcePath, List<Expression>> resources = new HashMap<>();
		Map<Integer, Type> variables = new HashMap<>();
		Map<Channel, Map<Integer, Map.Entry<List<Expression>, Type>>> messages = new HashMap<>();
		Map<Integer, Type> consOrSet = new HashMap<>();
		Map<Integer, Type> tuple = new HashMap<>();
		Map<Integer, Type> pair = new HashMap<>();
		Map<Integer, Type> map = new HashMap<>();
		Map<Integer, Type> json = new HashMap<>();
		
		// Maps from the objectId of each expression to its belonging group that has the same type as the expression
		Map<Integer, List<Expression>> expToResource = new HashMap<>();
		Map<Integer, List<Expression>> expToVariable = new HashMap<>();
		Map<Integer, List<Expression>> expToMessage = new HashMap<>();
		Map<Integer, Set<List<Expression>>> expToConsOrSet = new HashMap<>();
		Map<Integer, Set<List<Expression>>> expToTuple = new HashMap<>();
		Map<Integer, Set<List<Expression>>> expToPair = new HashMap<>();
		Map<Integer, Set<List<Expression>>> expToMap = new HashMap<>();
		Map<Integer, Set<List<Expression>>> expToJson = new HashMap<>();

		// Maps from the objectId of each group to the set of updated expressions.
		Map<Integer, Map<Integer, Expression>> updateFromResource = new HashMap<>();
		Map<Integer, Map<Integer, Expression>> updateFromVariable = new HashMap<>();
		Map<Integer, Map<Integer, Expression>> updateFromMessage = new HashMap<>();
		Map<Integer, Map<Integer, Expression>> updateFromConsOrSet = new HashMap<>();
		Map<Integer, Map<Integer, Expression>> updateFromTuple = new HashMap<>();
		Map<Integer, Map<Integer, Expression>> updateFromPair = new HashMap<>();
		Map<Integer, Map<Integer, Expression>> updateFromMap = new HashMap<>();
		Map<Integer, Map<Integer, Expression>> updateFromJson = new HashMap<>();

		listComponentTypes.put(DataConstraintModel.typeList, null);
		listComponentTypes.put(DataConstraintModel.typeListInt, DataConstraintModel.typeInt);
		listComponentTypes.put(DataConstraintModel.typeListStr, DataConstraintModel.typeString);
		listTypes.put(DataConstraintModel.typeInt, DataConstraintModel.typeListInt);
		listTypes.put(DataConstraintModel.typeString, DataConstraintModel.typeListStr);
		pairComponentTypes.put(DataConstraintModel.typePair, null);
		pairComponentTypes.put(DataConstraintModel.typePairInt, DataConstraintModel.typeInt);
		pairComponentTypes.put(DataConstraintModel.typePairStr, DataConstraintModel.typeString);
		pairComponentTypes.put(DataConstraintModel.typePairDouble, DataConstraintModel.typeDouble);
		pairTypes.put(DataConstraintModel.typeInt, DataConstraintModel.typePairInt);
		pairTypes.put(DataConstraintModel.typeString, DataConstraintModel.typePairStr);
		pairTypes.put(DataConstraintModel.typeDouble, DataConstraintModel.typePairDouble);
		tupleComponentTypes.put(DataConstraintModel.typeTuple, Arrays.asList(new Type[] { null, null }));
		mapComponentTypes.put(DataConstraintModel.typeMap, Arrays.asList(new Type[] { null, null }));

		// 1. Collect type information from the architecture model.
		Collection<Channel> channels = new HashSet<>(model.getIOChannels());
		channels.addAll(model.getChannels());
		for (Channel c : channels) {
			for (ChannelMember cm : c.getChannelMembers()) {
				StateTransition st = cm.getStateTransition();
				ResourcePath res = cm.getResource();
				
				// 1.1 Group expressions by resources.
				List<Expression> sameResource = resources.get(res);
				if (sameResource == null) {
					sameResource = new ArrayList<>();
					resources.put(res, sameResource);
				}
				sameResource.add(st.getCurStateExpression());
				if (st.getNextStateExpression() != null) sameResource.add(st.getNextStateExpression());
				expToResource.put(System.identityHashCode(st.getCurStateExpression()), sameResource);
				if (st.getNextStateExpression() != null) expToResource.put(System.identityHashCode(st.getNextStateExpression()), sameResource);
				Map<Integer, Expression> updatedExps = getUpdateSet(updateFromResource, sameResource);
				Type resType = res.getResourceStateType();
				Expression exp = st.getCurStateExpression();
				Type expType = getExpTypeIfUpdatable(resType, exp);
				if (expType != null) {
					res.setResourceStateType(expType);
					for (Expression resExp : sameResource) {
						if (resExp != exp) {
							if (resExp instanceof Variable && compareTypes(((Variable) resExp).getType(), expType)) {
								((Variable) resExp).setType(expType);
								updatedExps.put(System.identityHashCode(resExp), resExp);
							} else if (resExp instanceof Term && compareTypes(((Term) resExp).getType(), expType)) {
								((Term) resExp).setType(expType);
								updatedExps.put(System.identityHashCode(resExp), resExp);
							}
						}
					}
				} else if (exp instanceof Variable) {
					if (compareTypes(((Variable) exp).getType(), resType)) {
						((Variable) exp).setType(resType);
						updatedExps.put(System.identityHashCode(exp), exp);
					}
				} else if (exp instanceof Term) {
					if (compareTypes(((Term) exp).getType(), resType)) {
						((Term) exp).setType(resType);
						updatedExps.put(System.identityHashCode(exp), exp);
					}
				}
				resType = res.getResourceStateType();
				exp = st.getNextStateExpression();
				if (exp != null) {
					expType = getExpTypeIfUpdatable(resType, exp);
					if (expType != null) {
						res.setResourceStateType(expType);
						for (Expression resExp : sameResource) {
							if (resExp != exp) {
								if (resExp instanceof Variable && compareTypes(((Variable) resExp).getType(), expType)) {
									((Variable) resExp).setType(expType);
									updatedExps.put(System.identityHashCode(resExp), resExp);
								} else if (resExp instanceof Term && compareTypes(((Term) resExp).getType(), expType)) {
									((Term) resExp).setType(expType);
									updatedExps.put(System.identityHashCode(resExp), resExp);
								}
							}
						}
					} else if (exp instanceof Variable) {
						if (compareTypes(((Variable) exp).getType(), resType)) {
							((Variable) exp).setType(resType);
							updatedExps.put(System.identityHashCode(exp), exp);
						}
					} else if (exp instanceof Term) {
						if (compareTypes(((Term) exp).getType(), resType)) {
							((Term) exp).setType(resType);
							updatedExps.put(System.identityHashCode(exp), exp);
						}
					}
				}
				
				// 1.2 Group expressions by variable.
				Map<String, List<Expression>> locals = new HashMap<>();
				Map<String, Type> localTypes = new HashMap<>();
				List<Variable> allVariables = new ArrayList<>();
				allVariables.addAll(st.getCurStateExpression().getVariables().values());
				allVariables.addAll(st.getMessageExpression().getVariables().values());
				if (st.getNextStateExpression() != null)
					allVariables.addAll(st.getNextStateExpression().getVariables().values());
				for (Variable var : allVariables) {
					List<Expression> sameVariable = locals.get(var.getName());
					if (sameVariable == null) {
						sameVariable = new ArrayList<>();
						sameVariable.add(var);
						expToVariable.put(System.identityHashCode(var), sameVariable);
						locals.put(var.getName(), sameVariable);
						localTypes.put(var.getName(), var.getType());
					} else {
						sameVariable.add(var);
						expToVariable.put(System.identityHashCode(var), sameVariable);
						Type varType = localTypes.get(var.getName());
						Map<Integer, Expression> updatedVars = getUpdateSet(updateFromVariable, sameVariable);
						if (compareTypes(varType, var.getType())) {
							localTypes.put(var.getName(), var.getType());
							for (Expression v : sameVariable) {
								if (v != var) {
									if (compareTypes(((Variable) v).getType(), var.getType())) {
										((Variable) v).setType(var.getType());
										updatedVars.put(System.identityHashCode(v), v);
									}
								}
							}
						} else if (compareTypes(var.getType(), varType)) {
							var.setType(varType);
							updatedVars.put(System.identityHashCode(var), var);
						}
					}
				}
				for (String varName : locals.keySet()) {
					variables.put(System.identityHashCode(locals.get(varName)), localTypes.get(varName));
				}
				
				// 1.3 Group expressions by message.
				Expression message = st.getMessageExpression();
				if (message instanceof Variable) {
					Type msgType = ((Variable) message).getType();
					Map<Integer, Map.Entry<List<Expression>, Type>> msgTypeMap = messages.get(c);
					if (msgTypeMap == null) {
						msgTypeMap = new HashMap<>();
						messages.put(c, msgTypeMap);
					}
					Map.Entry<List<Expression>, Type> typeAndExps = msgTypeMap.get(0);
					if (typeAndExps == null) {
						List<Expression> exps = new ArrayList<>();
						exps.add(message);
						typeAndExps = new AbstractMap.SimpleEntry<>(exps, msgType);
						msgTypeMap.put(0, typeAndExps);
						expToMessage.put(System.identityHashCode(message), exps);
					} else {
						typeAndExps.getKey().add(message);
						expToMessage.put(System.identityHashCode(message), typeAndExps.getKey());
						Map<Integer, Expression> updateExps = getUpdateSet(updateFromMessage, typeAndExps.getKey());
						if (compareTypes(typeAndExps.getValue(), msgType)) {
							typeAndExps.setValue(msgType);
							for (Expression e : typeAndExps.getKey()) {
								if (e != message) {
									if (e instanceof Variable) {
										((Variable) e).setType(msgType);
										updateExps.put(System.identityHashCode(e), e);
									}
								}
							}
						} else if (compareTypes(msgType, typeAndExps.getValue())) {
							((Variable) message).setType(typeAndExps.getValue());
							updateExps.put(System.identityHashCode(message), message);
						}
					}
				} else if (message instanceof Term) {
					Map<Integer, Map.Entry<List<Expression>, Type>> msgTypeMap = messages.get(c);
					if (msgTypeMap == null) {
						msgTypeMap = new HashMap<>();
						messages.put(c, msgTypeMap);
					}
					for (int i = 0; i < ((Term) message).getArity(); i++) {
						Expression arg = ((Term) message).getChild(i);
						Type argType = null;
						if (arg instanceof Variable) {
							argType = ((Variable) arg).getType();
						} else if (arg instanceof Term) {
							argType = ((Term) arg).getType();
						} else {
							continue;
						}
						Map.Entry<List<Expression>, Type> typeAndExps = msgTypeMap.get(i);
						if (typeAndExps == null) {
							List<Expression> exps = new ArrayList<>();
							exps.add(arg);
							typeAndExps = new AbstractMap.SimpleEntry<>(exps, argType);
							msgTypeMap.put(i, typeAndExps);
							expToMessage.put(System.identityHashCode(arg), exps);
						} else {
							typeAndExps.getKey().add(arg);
							expToMessage.put(System.identityHashCode(arg), typeAndExps.getKey());
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromMessage, typeAndExps.getKey());
							if (compareTypes(typeAndExps.getValue(), argType)) {
								typeAndExps.setValue(argType);
								for (Expression e : typeAndExps.getKey()) {
									if (e != arg) {
										if (e instanceof Variable) {
											((Variable) e).setType(argType);
											updateExps.put(System.identityHashCode(e), e);
										}
									}
								}
							} else if (compareTypes(argType, typeAndExps.getValue())) {
								if (arg instanceof Variable) {
									((Variable) arg).setType(typeAndExps.getValue());
									updateExps.put(System.identityHashCode(arg), arg);
								} else if (arg instanceof Term) {
									((Term) arg).setType(typeAndExps.getValue());
									updateExps.put(System.identityHashCode(arg), arg);
								}
							}
						}
					}
				}
				
				// 1.4 Extract constraints on expressions in each term.
				List<Term> terms = new ArrayList<>();
				if (st.getCurStateExpression() instanceof Term) {
					Map<Position, Term> subTerms = ((Term) st.getCurStateExpression()).getSubTerms(Term.class);
					terms.addAll(subTerms.values());
				}
				if (st.getMessageExpression() instanceof Term) {
					Map<Position, Term> subTerms = ((Term) st.getMessageExpression()).getSubTerms(Term.class);
					terms.addAll(subTerms.values());
				}
				if (st.getNextStateExpression() != null && st.getNextStateExpression() instanceof Term) {
					Map<Position, Term> subTerms = ((Term) st.getNextStateExpression()).getSubTerms(Term.class);
					terms.addAll(subTerms.values());
				}
				for (Term t : terms) {
					Symbol symbol = t.getSymbol();
					if (symbol.equals(DataConstraintModel.cons) || symbol.equals(DataConstraintModel.set)) {
						// If the root symbol of the term is cons or set.
						List<Expression> consExps = new ArrayList<>();
						consExps.add(t);	// list term
						updateExpressionBelonging(expToConsOrSet, t, consExps);
						if (symbol.equals(DataConstraintModel.cons)) {
							// If the root symbol of the term is cons.
							for (Expression e : t.getChildren()) {
								consExps.add(e);
								updateExpressionBelonging(expToConsOrSet, e, consExps);
							}
						} else {
							// If the root symbol of the term is set.
							Expression e = t.getChildren().get(2);
							consExps.add(e);	// list component
							updateExpressionBelonging(expToConsOrSet, e, consExps);
							e = t.getChildren().get(0);
							consExps.add(e);	// list argument
							updateExpressionBelonging(expToConsOrSet, e, consExps);							
						}
						Type newType = getExpTypeIfUpdatable(t.getType(), consExps.get(2));
						if (newType != null) {
							// If the type of the 2nd argument of cons (1st argument of set) is more concrete than the type of the term.
							t.setType(newType);
							Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
							updateCons.put(System.identityHashCode(t), t);
						} else {
							Type arg2Type = null;
							if (consExps.get(2) != null && consExps.get(2) instanceof Variable) {
								arg2Type = ((Variable) consExps.get(2)).getType();
								if (compareTypes(arg2Type, t.getType())) {
									// If the type of the term is more concrete than the type of the 2nd argument of cons (1st argument of set).
									((Variable) consExps.get(2)).setType(t.getType());
									Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
									updateCons.put(System.identityHashCode(consExps.get(2)), consExps.get(2));
								}
							} else if (consExps.get(2) != null && consExps.get(2) instanceof Term) {
								arg2Type = ((Term) consExps.get(2)).getType();
								if (compareTypes(arg2Type, t.getType())) {
									// If the type of the term is more concrete than the type of the 2nd argument of cons (1st argument of set).
									((Term) consExps.get(2)).setType(t.getType());
									Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
									updateCons.put(System.identityHashCode(consExps.get(2)), consExps.get(2));
								}
							}
						}
						Type newCompType = getExpTypeIfUpdatable(listComponentTypes.get(t.getType()), consExps.get(1));
						if (newCompType != null) {
							// If the type of the 1st argument of cons (3rd argument of set) is more concrete than the type of list component.
							Type newListType = listTypes.get(newCompType);
							if (newListType == null) {
								// Create new list type.
								newListType = createNewListType(newCompType, DataConstraintModel.typeList);
							}
							t.setType(newListType);
							Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
							updateCons.put(System.identityHashCode(t), t);
							if (consExps.get(2) != null && consExps.get(2) instanceof Variable) {
								((Variable) consExps.get(2)).setType(newListType);
								updateCons.put(System.identityHashCode(consExps.get(2)), consExps.get(2));
							} else if (consExps.get(2) != null && consExps.get(2) instanceof Term) {
								((Term) consExps.get(2)).setType(newListType);
								updateCons.put(System.identityHashCode(consExps.get(2)), consExps.get(2));
							}
						}
						consOrSet.put(System.identityHashCode(consExps), t.getType());
					} else if (symbol.equals(DataConstraintModel.head) || symbol.equals(DataConstraintModel.get)) {
						// If the root symbol of the term is head or get.
						List<Expression> consExps = new ArrayList<>();
						Expression e = t.getChildren().get(0);
						consExps.add(e);		// list argument
						updateExpressionBelonging(expToConsOrSet, e, consExps);
						consExps.add(t);		// list's component
						updateExpressionBelonging(expToConsOrSet, t, consExps);
						consExps.add(null);
						Type listType = listTypes.get(t.getType());
						if (listType == null && t.getType() != null) {
							// Create a new list type.
							listType = createNewListType(t.getType(), DataConstraintModel.typeList);
						}
						Type newListType = getExpTypeIfUpdatable(listType, consExps.get(0));
						if (newListType != null) {
							// If the type of the component of the 1st argument is more concrete than the type of the term.
							Type newCompType = listComponentTypes.get(newListType);
							if (newCompType != null) {
								t.setType(newCompType);
								Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
								updateCons.put(System.identityHashCode(t), t);
							}
							consOrSet.put(System.identityHashCode(consExps), newListType);						
						} else {
							// If the type of the term is more concrete than the type of the component of the 1st argument.
							if (consExps.get(0) != null && consExps.get(0) instanceof Variable) {
								((Variable) consExps.get(0)).setType(listType);
								Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
								updateCons.put(System.identityHashCode(consExps.get(0)), consExps.get(0));
							} else if (consExps.get(0) != null && consExps.get(0) instanceof Term) {
								((Term) consExps.get(0)).setType(listType);
								Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
								updateCons.put(System.identityHashCode(consExps.get(0)), consExps.get(0));
							}
							consOrSet.put(System.identityHashCode(consExps), listType);						
						}
					} else if (symbol.equals(DataConstraintModel.tail)) {
						// If the root symbol of the term is tail.
						List<Expression> consExps = new ArrayList<>();
						consExps.add(t);		// list term
						updateExpressionBelonging(expToConsOrSet, t, consExps);
						consExps.add(null);		// list's component
						Expression e = t.getChildren().get(0);
						consExps.add(e);		// list argument
						updateExpressionBelonging(expToConsOrSet, e, consExps);							
						Type newType = getExpTypeIfUpdatable(t.getType(), consExps.get(2));
						if (newType != null) {
							// If the type of the argument is more concrete than the type of the term.
							t.setType(newType);
							Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
							updateCons.put(System.identityHashCode(t), t);
						} else {
							Type argType = null;
							if (consExps.get(2) != null && consExps.get(2) instanceof Variable) {
								argType = ((Variable) consExps.get(2)).getType();
								if (compareTypes(argType, t.getType())) {
									// If the type of the term is more concrete than the type of the argument.
									((Variable) consExps.get(2)).setType(t.getType());
									Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
									updateCons.put(System.identityHashCode(consExps.get(2)), consExps.get(2));
								}
							} else if (consExps.get(2) != null && consExps.get(2) instanceof Term) {
								argType = ((Term) consExps.get(2)).getType();
								if (compareTypes(argType, t.getType())) {
									// If the type of the term is more concrete than the type of the argument.
									((Term) consExps.get(2)).setType(t.getType());
									Map<Integer, Expression> updateCons = getUpdateSet(updateFromConsOrSet, consExps);
									updateCons.put(System.identityHashCode(consExps.get(2)), consExps.get(2));
								}
							}						
						}
						consOrSet.put(System.identityHashCode(consExps), t.getType());
					} else if (symbol.equals(DataConstraintModel.tuple)) {
						// If the root symbol of the term is tuple.
						List<Expression> tupleExps = new ArrayList<>();
						List<Type> newArgTypesList = new ArrayList<>();
						tupleExps.add(t);	// tuple term
						updateExpressionBelonging(expToTuple, t, tupleExps);
						for (Expression e : t.getChildren()) {
							tupleExps.add(e);	// tuple's component
							updateExpressionBelonging(expToTuple, e, tupleExps);
							if (e instanceof Variable) {
								newArgTypesList.add(((Variable) e).getType());
							} else if (e instanceof Term) {
								newArgTypesList.add(((Term) e).getType());
							} else {
								newArgTypesList.add(null);
							}
						}
						if (t.getType() == DataConstraintModel.typeTuple) {
							Type newTupleType = tupleTypes.get(newArgTypesList);
							if (newTupleType == null) {
								// Create new tuple type;
								newTupleType = createNewTupleType(newArgTypesList, DataConstraintModel.typeTuple);
							}
							// Update the type of the tuple term and record the updated expression.
							t.setType(newTupleType);
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromTuple, tupleExps);
							updateExps.put(System.identityHashCode(t), t);
						}
						tuple.put(System.identityHashCode(tupleExps), t.getType());
					} else if (symbol.equals(DataConstraintModel.pair)) {
						// If the root symbol of the term is pair.
						List<Expression> pairExps = new ArrayList<>();
						pairExps.add(t);		// pair
						updateExpressionBelonging(expToPair, t, pairExps);
						if (t.getType() == DataConstraintModel.typePair) {
							for (Expression e : t.getChildren()) {
								pairExps.add(e);	// left/right
								updateExpressionBelonging(expToPair, e, pairExps);
								Type newArgType = null;
								if (e instanceof Variable) {
									newArgType = (((Variable) e).getType());
									
								} else if (e instanceof Term) {
									newArgType = (((Term) e).getType());
								}
								
								if (newArgType != null) {
									Type newPairType = pairTypes.get(newArgType);
									if (newPairType != null) {
										t.setType(newPairType);
										Map<Integer, Expression> updateExps = getUpdateSet(updateFromPair, pairExps);
										updateExps.put(System.identityHashCode(t), t);
									}
								}
							}
							pair.put(System.identityHashCode(pairExps), t.getType());
							
						}
					} else if (symbol.equals(DataConstraintModel.fst)) {
						// If the root symbol of the term is fst.
						List<Expression> tupleExps = new ArrayList<>();
						Expression arg = t.getChildren().get(0);
						tupleExps.add(arg);		// tuple argument
						updateExpressionBelonging(expToTuple, arg, tupleExps);
						tupleExps.add(t);		// first component
						updateExpressionBelonging(expToTuple, t, tupleExps);
						tupleExps.add(null);	// second component
						Type argType = null;
						if (arg instanceof Variable) {
							argType = ((Variable) arg).getType();
						} else if (arg instanceof Term) {
							argType = ((Term) arg).getType();
						}
						Type newTupleType = DataConstraintModel.typeTuple;
						if (argType == DataConstraintModel.typeTuple && t.getType() != null) {
							List<Type> newCompTypeList = new ArrayList<>();
							newCompTypeList.add(t.getType());
							newCompTypeList.add(null);
							newTupleType = tupleTypes.get(newCompTypeList);
							if (newTupleType == null) {
								// Create new tuple type;
								newTupleType = createNewTupleType(newCompTypeList, DataConstraintModel.typeTuple);
							}
						}
						if (argType != newTupleType && newTupleType != null) {
							// Update the type of the tuple argument and record the updated expression.
							if (arg instanceof Variable) {
								((Variable) arg).setType(newTupleType);
								argType = newTupleType;
							} else if (arg instanceof Term) {
								((Term) arg).setType(newTupleType);
								argType = newTupleType;
							}
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromTuple, tupleExps);
							updateExps.put(System.identityHashCode(arg), arg);
						}
						tuple.put(System.identityHashCode(tupleExps), argType);
					} else if (symbol.equals(DataConstraintModel.snd)) {
						// If the root symbol of the term is snd.
						List<Expression> tupleExps = new ArrayList<>();
						Expression arg = t.getChildren().get(0);
						tupleExps.add(arg);		// tuple argument
						updateExpressionBelonging(expToTuple, arg, tupleExps);
						tupleExps.add(null);	// first component
						tupleExps.add(t);		// second component
						updateExpressionBelonging(expToTuple, t, tupleExps);
						Type argType = null;
						if (arg instanceof Variable) {
							argType = ((Variable) arg).getType();
						} else if (arg instanceof Term) {
							argType = ((Term) arg).getType();
						}
						Type newTupleType = DataConstraintModel.typeTuple;
						if (argType == DataConstraintModel.typeTuple && t.getType() != null) {
							List<Type> newCompTypeList = new ArrayList<>();
							newCompTypeList.add(null);
							if (DataConstraintModel.typeTuple.isAncestorOf(t.getType())) {
								List<Type> sndTypes = tupleComponentTypes.get(t.getType());
								if (sndTypes != null) {
									for (Type t2: sndTypes) {
										newCompTypeList.add(t2);
									}
								} else {
									newCompTypeList.add(t.getType());
								}
							} else {
								newCompTypeList.add(t.getType());
							}
							newTupleType = tupleTypes.get(newCompTypeList);
							if (newTupleType == null) {
								// Create new tuple type;
								newTupleType = createNewTupleType(newCompTypeList, DataConstraintModel.typeTuple);
							}
						}
						if (argType != newTupleType && newTupleType != null) {
							// Update the type of the tuple argument and record the updated expression.
							if (arg instanceof Variable) {
								((Variable) arg).setType(newTupleType);
								argType = newTupleType;
							} else if (arg instanceof Term) {
								((Term) arg).setType(newTupleType);
								argType = newTupleType;
							}
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromTuple, tupleExps);
							updateExps.put(System.identityHashCode(arg), arg);
						}
						tuple.put(System.identityHashCode(tupleExps), argType);
					} else if (symbol.equals(DataConstraintModel.left)) {
						// If the root symbol of the term is left.
						List<Expression> pairExps = new ArrayList<>();
						Expression arg = t.getChildren().get(0);
						pairExps.add(arg);		// pair
						updateExpressionBelonging(expToPair, arg, pairExps);
						pairExps.add(t);		// left
						updateExpressionBelonging(expToPair, t, pairExps);
						pairExps.add(null);		// right
						Type argType = null;
						if (arg instanceof Variable) {
							argType = ((Variable) arg).getType();
						} else if (arg instanceof Term) {
							argType = ((Term) arg).getType();
						}
						Type newPairType = DataConstraintModel.typePair;
						if (argType == DataConstraintModel.typePair && t.getType() != null) {
							List<Type> newCompTypeList = new ArrayList<>();
							newCompTypeList.add(t.getType());
							newCompTypeList.add(null);
							newPairType = pairTypes.get(newCompTypeList);
							if (newPairType == null) {
								// Create new tuple type;
								newPairType = createNewTupleType(newCompTypeList, DataConstraintModel.typePair);
							}
						}
						if (argType != newPairType && newPairType != null) {
							if (arg instanceof Variable) {
								((Variable) arg).setType(newPairType);
								argType = newPairType;
							} else if (arg instanceof Term) {
								((Term) arg).setType(newPairType);
								argType = newPairType;
							}
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromPair, pairExps);
							updateExps.put(System.identityHashCode(arg), arg);
						}
						pair.put(System.identityHashCode(pairExps), argType);
					} else if (symbol.equals(DataConstraintModel.right)) {
						// If the root symbol of the term is right.
						List<Expression> pairExps = new ArrayList<>();
						Expression arg = t.getChildren().get(0);
						pairExps.add(arg);		// pair
						updateExpressionBelonging(expToPair, arg, pairExps);
						pairExps.add(null);		// left
						pairExps.add(t);		// right
						updateExpressionBelonging(expToPair, t, pairExps);
						Type argType = null;
						if (arg instanceof Variable) {
							argType = ((Variable) arg).getType();
						} else if (arg instanceof Term) {
							argType = ((Term) arg).getType();
						}
						Type newPairType = DataConstraintModel.typePair;
						if (argType == DataConstraintModel.typePair && t.getType() != null) {
							List<Type> newCompTypeList = new ArrayList<>();
							newCompTypeList.add(null);
							newCompTypeList.add(t.getType());
							newPairType = pairTypes.get(newCompTypeList);
							if (newPairType == null) {
								// Create new tuple type;
								newPairType = createNewTupleType(newCompTypeList, DataConstraintModel.typePair);
							}
						}
						if (argType != newPairType && newPairType != null) {
							if (arg instanceof Variable) {
								((Variable) arg).setType(newPairType);
								argType = newPairType;
							} else if (arg instanceof Term) {
								((Term) arg).setType(newPairType);
								argType = newPairType;
							}
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromPair, pairExps);
							updateExps.put(System.identityHashCode(arg), arg);
						}
						pair.put(System.identityHashCode(pairExps), argType);
					} else if (symbol.equals(DataConstraintModel.lookup)) {
						// If the root symbol of the term is lookup.
						List<Expression> mapExps = new ArrayList<>();
						Expression arg1 = t.getChildren().get(0);	// map
						mapExps.add(arg1);
						updateExpressionBelonging(expToMap, arg1, mapExps);
						Expression arg2 = t.getChildren().get(1);	// key
						mapExps.add(arg2);
						updateExpressionBelonging(expToMap, arg2, mapExps);
						mapExps.add(t);								// value
						updateExpressionBelonging(expToMap, t, mapExps);
						Type arg1Type = null;
						if (arg1 instanceof Variable) {
							arg1Type = ((Variable) arg1).getType();
						} else if (arg1 instanceof Term) {
							arg1Type = ((Term) arg1).getType();
						}
						List<Type> newCompTypeList = new ArrayList<>();
						if (arg2 instanceof Variable) {
							newCompTypeList.add(((Variable) arg2).getType());
						} else if (arg2 instanceof Term) {
							newCompTypeList.add(((Term) arg2).getType());
						} else {
							newCompTypeList.add(null);
						}
						newCompTypeList.add(t.getType());
						if (arg1Type == DataConstraintModel.typeMap || arg1Type == null) {
							Type newMapType = mapTypes.get(newCompTypeList);
							if (newMapType == null) {
								// Create new tuple type;
								newMapType = createNewMapType(newCompTypeList, DataConstraintModel.typeMap);
							}
							// Update the type of the map argument and record the updated expression.
							if (arg1 instanceof Variable) {
								((Variable) arg1).setType(newMapType);
								arg1Type = newMapType;
							} else if (arg1 instanceof Term) {
								((Term) arg1).setType(newMapType);
								arg1Type = newMapType;
							}							
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromMap, mapExps);
							updateExps.put(System.identityHashCode(arg1), arg1);
						}
						map.put(System.identityHashCode(mapExps), arg1Type);
					} else if (symbol.equals(DataConstraintModel.insert)) {
						// If the root symbol of the term is insert.
						List<Expression> mapExps = new ArrayList<>();
						mapExps.add(t);								// map
						updateExpressionBelonging(expToMap, t, mapExps);
						Expression arg1 = t.getChildren().get(1);	// key
						mapExps.add(arg1);
						updateExpressionBelonging(expToMap, arg1, mapExps);
						Expression arg2 = t.getChildren().get(2);	// value
						mapExps.add(arg2);
						updateExpressionBelonging(expToMap, arg2, mapExps);
						Expression arg0 = t.getChildren().get(0);	// map
						mapExps.add(arg0);
						updateExpressionBelonging(expToMap, arg0, mapExps);
						Type termType = t.getType();
						List<Type> newCompTypeList = new ArrayList<>();
						if (arg1 instanceof Variable) {
							newCompTypeList.add(((Variable) arg1).getType());
						} else if (arg1 instanceof Term) {
							newCompTypeList.add(((Term) arg1).getType());
						} else {
							newCompTypeList.add(null);
						}
						if (arg2 instanceof Variable) {
							newCompTypeList.add(((Variable) arg2).getType());
						} else if (arg2 instanceof Term) {
							newCompTypeList.add(((Term) arg2).getType());
						} else {
							newCompTypeList.add(null);
						}
						if (termType == DataConstraintModel.typeMap || termType == null) {
							Type newMapType = mapTypes.get(newCompTypeList);
							if (newMapType == null) {
								// Create new tuple type;
								newMapType = createNewMapType(newCompTypeList, DataConstraintModel.typeMap);
							}
							// Update the type of the map term and record the updated expression.
							t.setType(newMapType);
							termType = newMapType;
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromMap, mapExps);
							updateExps.put(System.identityHashCode(t), t);
						}
						map.put(System.identityHashCode(mapExps), termType);
					} else if (symbol.equals(DataConstraintModel.addMember)) {
						// If the root symbol of the term is addMember (addMember(json, key, value)).
						List<Expression> dotExps = new ArrayList<>();
						Expression jsonArg = t.getChildren().get(0);
						Expression keyArg = t.getChildren().get(1);
						Expression valueArg = t.getChildren().get(2);
						dotExps.add(t);			// json
						updateExpressionBelonging(expToJson, t, dotExps);
						dotExps.add(keyArg);	// key
						updateExpressionBelonging(expToJson, keyArg, dotExps);
						dotExps.add(valueArg);	// value
						updateExpressionBelonging(expToJson, valueArg, dotExps);
						dotExps.add(jsonArg);	// json
						updateExpressionBelonging(expToJson, jsonArg, dotExps);
						Type jsonType = t.getType();
						Type valueType = null;
						if (valueArg instanceof Variable) {
							valueType = ((Variable) valueArg).getType();
						} else if (valueArg instanceof Term) {
							valueType = ((Term) valueArg).getType();
						}
						String keyName = null;
						if (keyArg instanceof Constant) {
							keyName = ((Constant) keyArg).getSymbol().getName();
						}
						Type jsonArgType = null;
						if (jsonArg instanceof Variable) {
							jsonArgType = ((Variable) jsonArg).getType();
						} else if (jsonArg instanceof Term) {
							jsonArgType = ((Term) jsonArg).getType();
						}
						Type newJsonType = DataConstraintModel.typeJson;
						if (jsonType == DataConstraintModel.typeJson && jsonArgType != null && keyName != null) {
							Map<String, Type> newMemberTypes = new HashMap<>(((JsonType) jsonArgType).getMemberTypes());
							newMemberTypes.put(keyName, valueType);
							newJsonType = jsonTypes.get(newMemberTypes);
							if (newJsonType == null) {
								// Create new json type;
								newJsonType = createNewJsonType(newMemberTypes, DataConstraintModel.typeJson);
							}
						}
						if (jsonType != newJsonType && newJsonType != null) {
							// Update the type of the json term and record the updated expression.
							t.setType(newJsonType);
							jsonType = newJsonType;
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromJson, dotExps);
							updateExps.put(System.identityHashCode(t), t);
						}
						json.put(System.identityHashCode(dotExps), jsonType);
					} else if (symbol.equals(DataConstraintModel.dot)) {
						// If the root symbol of the term is dot (json.property).
						List<Expression> dotExps = new ArrayList<>();
						Expression jsonArg = t.getChildren().get(0);
						Expression keyArg = t.getChildren().get(1);
						dotExps.add(jsonArg);	// json
						updateExpressionBelonging(expToJson, jsonArg, dotExps);
						dotExps.add(keyArg);	// key
						updateExpressionBelonging(expToJson, keyArg, dotExps);
						dotExps.add(t);			// value
						updateExpressionBelonging(expToJson, t, dotExps);
						dotExps.add(null);		// json
						Type jsonType = null;
						if (jsonArg instanceof Variable) {
							jsonType = ((Variable) jsonArg).getType();
						} else if (jsonArg instanceof Term) {
							jsonType = ((Term) jsonArg).getType();
						}
						String keyName = null;
						if (keyArg instanceof Constant) {
							keyName = ((Constant) keyArg).getSymbol().getName();
						}
						Type newJsonType = DataConstraintModel.typeJson;
						if (jsonType == DataConstraintModel.typeJson && t.getType() != null && keyName != null) {
							Map<String, Type> newMemberTypes = new HashMap<>();
							newMemberTypes.put(keyName, t.getType());
							newJsonType = jsonTypes.get(newMemberTypes);
							if (newJsonType == null) {
								// Create new json type;
								newJsonType = createNewJsonType(newMemberTypes, DataConstraintModel.typeJson);
							}
						}
						if (jsonType != newJsonType && newJsonType != null) {
							// Update the type of the json argument and record the updated expression.
							if (jsonArg instanceof Variable) {
								((Variable) jsonArg).setType(newJsonType);
								jsonType = newJsonType;
							} else if (jsonArg instanceof Term) {
								((Term) jsonArg).setType(newJsonType);
								jsonType = newJsonType;
							}
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromJson, dotExps);
							updateExps.put(System.identityHashCode(jsonArg), jsonArg);
						}
						json.put(System.identityHashCode(dotExps), jsonType);
					} else if (symbol.equals(DataConstraintModel.dotParam)) {
						// If the root symbol of the term is dot (json.{param}).
						List<Expression> dotExps = new ArrayList<>();
						Expression jsonArg = t.getChildren().get(0);
						Expression keyArg = t.getChildren().get(1);
						dotExps.add(jsonArg);	// json (list/map)
						updateExpressionBelonging(expToJson, jsonArg, dotExps);
						dotExps.add(null);		// key
						dotExps.add(t);			// value
						updateExpressionBelonging(expToJson, t, dotExps);
						dotExps.add(null);		// json
						Type jsonType = null;
						if (jsonArg instanceof Variable) {
							jsonType = ((Variable) jsonArg).getType();
						} else if (jsonArg instanceof Term) {
							jsonType = ((Term) jsonArg).getType();
						}
						Type keyType = null;
						if (keyArg instanceof Variable) {
							keyType = ((Variable) keyArg).getType();
						} else if (keyArg instanceof Term) {
							keyType = ((Term) keyArg).getType();
						}
						Type newJsonType = null;
						if (keyType == DataConstraintModel.typeInt) {
							newJsonType = DataConstraintModel.typeList;
						} else if (keyType == DataConstraintModel.typeString) {
							newJsonType = DataConstraintModel.typeMap;
						}
						if (t.getType() != null) {
							if ((jsonType == DataConstraintModel.typeList)) {
								newJsonType = listTypes.get(t.getType());
								if (newJsonType == null) {
									// Create new list type;
									newJsonType = createNewListType(t.getType(), DataConstraintModel.typeList);
								}
							} else if (jsonType == DataConstraintModel.typeMap) {
								List<Type> keyValueTypes = Arrays.asList(new Type[] {DataConstraintModel.typeString, t.getType()});
								newJsonType = mapTypes.get(keyValueTypes);
								if (newJsonType == null) {
									// Create new map type;
									newJsonType = createNewMapType(keyValueTypes, DataConstraintModel.typeMap);
								}
							}
						}
						if (jsonType != newJsonType && newJsonType != null) {
							// Update the type of the json argument and record the updated expression.
							if (jsonArg instanceof Variable) {
								((Variable) jsonArg).setType(newJsonType);
								jsonType = newJsonType;
							} else if (jsonArg instanceof Term) {
								((Term) jsonArg).setType(newJsonType);
								jsonType = newJsonType;
							}
							Map<Integer, Expression> updateExps = getUpdateSet(updateFromJson, dotExps);
							updateExps.put(System.identityHashCode(jsonArg), jsonArg);
						}
						json.put(System.identityHashCode(dotExps), jsonType);
					} else if (symbol.equals(DataConstraintModel.cond)) {
						// If the root symbol of the term is if function.
						Expression c1 = t.getChild(1);
						Expression c2 = t.getChild(2);
						List<Expression> condTerms = new ArrayList<>();
						condTerms.add(t);
						condTerms.add(c1);
						condTerms.add(c2);
						expToVariable.put(System.identityHashCode(t), condTerms);
						expToVariable.put(System.identityHashCode(c1), condTerms);
						expToVariable.put(System.identityHashCode(c2), condTerms);
						Type condType = t.getType();
						Map<Integer, Expression> updatedVars = getUpdateSet(updateFromVariable, condTerms);
						Type child1Type = getExpTypeIfUpdatable(condType, c1);
						if (child1Type != null) {
							condType = child1Type;
							t.setType(child1Type);
							updatedVars.put(System.identityHashCode(t), t);
						} else {
							if (c1 instanceof Variable && compareTypes(((Variable) c1).getType(), condType)) {
								((Variable) c1).setType(condType);
								updatedVars.put(System.identityHashCode(c1), c1);
							} else if (c1 instanceof Term && compareTypes(((Term) c1).getType(), condType)) {
								((Term) c1).setType(condType);
								updatedVars.put(System.identityHashCode(c1), c1);
							}
						}
						Type child2Type = getExpTypeIfUpdatable(condType, c2);
						if (child2Type != null) {
							condType = child2Type;
							t.setType(child2Type);
							updatedVars.put(System.identityHashCode(t), t);
							if (c1 instanceof Variable) {
								((Variable) c1).setType(child2Type);
								updatedVars.put(System.identityHashCode(c1), c1);
							} else if (c1 instanceof Term) {
								((Term) c1).setType(child2Type);
								updatedVars.put(System.identityHashCode(c1), c1);
							}
						} else {
							if (c2 instanceof Variable && compareTypes(((Variable) c2).getType(), condType)) {
								((Variable) c2).setType(condType);
								updatedVars.put(System.identityHashCode(c2), c2);
							} else if (c2 instanceof Term && compareTypes(((Term) c2).getType(), condType)) {
								((Term) c2).setType(condType);
								updatedVars.put(System.identityHashCode(c2), c2);
							}
						}
						variables.put(System.identityHashCode(condTerms), condType);
					} else if (symbol.equals(DataConstraintModel.add) || symbol.equals(DataConstraintModel.sub)
							|| symbol.equals(DataConstraintModel.mul) || symbol.equals(DataConstraintModel.div)) {
						// If the root symbol of the term is arithmetic operators.
						Expression c1 = t.getChild(0);
						Expression c2 = t.getChild(1);
						List<Expression> operands = new ArrayList<>();
						operands.add(t);
						operands.add(c1);
						operands.add(c2);
						expToVariable.put(System.identityHashCode(t), operands);
						expToVariable.put(System.identityHashCode(c1), operands);
						expToVariable.put(System.identityHashCode(c2), operands);
						Type opType = t.getType();
						Map<Integer, Expression> updatedVars = getUpdateSet(updateFromVariable, operands);
						Type child1Type = getExpTypeIfUpdatable(opType, c1);
						if (child1Type != null) {
							opType = child1Type;
							t.setType(child1Type);
							updatedVars.put(System.identityHashCode(t), t);
						} else {
							if (c1 instanceof Variable && compareTypes(((Variable) c1).getType(), opType)) {
								((Variable) c1).setType(opType);
								updatedVars.put(System.identityHashCode(c1), c1);
							} else if (c1 instanceof Term && compareTypes(((Term) c1).getType(), opType)) {
								((Term) c1).setType(opType);
								updatedVars.put(System.identityHashCode(c1), c1);
							}
						}
						Type child2Type = getExpTypeIfUpdatable(opType, c2);
						if (child2Type != null) {
							opType = child2Type;
							t.setType(child2Type);
							updatedVars.put(System.identityHashCode(t), t);
							if (c1 instanceof Variable) {
								((Variable) c1).setType(child2Type);
								updatedVars.put(System.identityHashCode(c1), c1);
							} else if (c1 instanceof Term) {
								((Term) c1).setType(child2Type);
								updatedVars.put(System.identityHashCode(c1), c1);
							}
						} else {
							if (c2 instanceof Variable && compareTypes(((Variable) c2).getType(), opType)) {
								((Variable) c2).setType(opType);
								updatedVars.put(System.identityHashCode(c2), c2);
							} else if (c2 instanceof Term && compareTypes(((Term) c2).getType(), opType)) {
								((Term) c2).setType(opType);
								updatedVars.put(System.identityHashCode(c2), c2);
							}
						}
						variables.put(System.identityHashCode(operands), opType);
					} else if (symbol.getSignature() != null
							&& symbol.getSignature()[0] == DataConstraintModel.typeList) {
						// If the root symbol of the term is the list type (except for the cons
						// function).
						List<Expression> consExps = new ArrayList<>();
						consExps.add(t);
						expToVariable.put(System.identityHashCode(t), consExps);
						Type condType = t.getType();
						Map<Integer, Expression> updatedVars = getUpdateSet(updateFromVariable, consExps);
						for (int i = 1; i < symbol.getSignature().length; i++) {
							Type tc = symbol.getSignature()[i];
							if (tc == DataConstraintModel.typeList) {
								Expression e = t.getChildren().get(i - 1);
								Type newType = getExpTypeIfUpdatable(condType, e);
								if (newType != null) {
									condType = newType;
									for (Expression e2 : consExps) {
										if (e2 instanceof Variable) {
											((Variable) e2).setType(newType);
											updatedVars.put(System.identityHashCode(e2), e2);
										} else if (e2 instanceof Term) {
											((Term) e2).setType(newType);
											updatedVars.put(System.identityHashCode(e2), e2);
										}
									}
								} else {
									if (e instanceof Variable && compareTypes(((Variable) e).getType(), condType)) {
										((Variable) e).setType(condType);
										updatedVars.put(System.identityHashCode(e), e);
									} else if (e instanceof Term && compareTypes(((Term) e).getType(), condType)) {
										((Term) e).setType(condType);
										updatedVars.put(System.identityHashCode(e), e);
									}
								}
								consExps.add(e);
								expToVariable.put(System.identityHashCode(e), consExps);
							}
						}
						variables.put(System.identityHashCode(consExps), condType);
					}
				}
			}
		}
		
		// 2. Propagate type information.
		while (updateFromResource.size() > 0 || updateFromVariable.size() > 0 || updateFromMessage.size() > 0
				|| updateFromConsOrSet.size() > 0 || updateFromTuple.size() > 0 || updateFromPair.size() > 0 
				|| updateFromMap.size() > 0 || updateFromJson.size() > 0) {
			if (updateFromResource.size() > 0) {
				Set<Integer> resourceKeys = updateFromResource.keySet();
				Integer resourceKey = resourceKeys.iterator().next();
				Map<Integer, Expression> resourceValue = updateFromResource.get(resourceKey);
				updateFromResource.remove(resourceKey);
				for (int i : resourceValue.keySet()) {
					Expression resExp = resourceValue.get(i);
					updateVaribleTypes(resExp, variables, expToVariable, updateFromVariable);
					updateMessageTypes(resExp, messages, expToMessage, updateFromMessage);
					updateConsOrSetTypes(resExp, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(resExp, tuple, expToTuple, updateFromTuple);
					updatePairTypes(resExp, pair, expToPair, updateFromPair);
					updateMapTypes(resExp, map, expToMap, updateFromMap);
					updateJsonTypes(resExp, json, expToJson, updateFromJson);
				}
			}
			if (updateFromVariable.size() > 0) {
				Set<Integer> variableKeys = updateFromVariable.keySet();
				Integer variableKey = variableKeys.iterator().next();
				Map<Integer, Expression> variableValue = updateFromVariable.get(variableKey);
				updateFromVariable.remove(variableKey);
				for (int i : variableValue.keySet()) {
					Expression var = variableValue.get(i);
					updateResourceTypes(var, resources, expToResource, updateFromResource);
					updateVaribleTypes(var, variables, expToVariable, updateFromVariable);
					updateMessageTypes(var, messages, expToMessage, updateFromMessage);
					updateConsOrSetTypes(var, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(var, tuple, expToTuple, updateFromTuple);
					updatePairTypes(var, pair, expToPair, updateFromPair);
					updateMapTypes(var, map, expToMap, updateFromMap);
					updateJsonTypes(var, json, expToJson, updateFromJson);
				}
			}
			if (updateFromMessage.size() > 0) {
				Set<Integer> messageKeys = updateFromMessage.keySet();
				Integer messageKey = messageKeys.iterator().next();
				Map<Integer, Expression> messageValue = updateFromMessage.get(messageKey);
				updateFromMessage.remove(messageKey);
				for (int i : messageValue.keySet()) {
					Expression mesExp = messageValue.get(i);
					updateResourceTypes(mesExp, resources, expToResource, updateFromResource);
					updateVaribleTypes(mesExp, variables, expToVariable, updateFromVariable);
					updateConsOrSetTypes(mesExp, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(mesExp, tuple, expToTuple, updateFromTuple);
					updatePairTypes(mesExp, pair, expToPair, updateFromPair);
					updateMapTypes(mesExp, map, expToMap, updateFromMap);
					updateJsonTypes(mesExp, json, expToJson, updateFromJson);
				}
			}
			if (updateFromConsOrSet.size() > 0) {
				Set<Integer> consKeys = updateFromConsOrSet.keySet();
				Integer consKey = consKeys.iterator().next();
				Map<Integer, Expression> consValue = updateFromConsOrSet.get(consKey);
				updateFromConsOrSet.remove(consKey);
				for (int i : consValue.keySet()) {
					Expression consExp = consValue.get(i);
					updateResourceTypes(consExp, resources, expToResource, updateFromResource);
					updateVaribleTypes(consExp, variables, expToVariable, updateFromVariable);
					updateMessageTypes(consExp, messages, expToMessage, updateFromMessage);
					updateConsOrSetTypes(consExp, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(consExp, tuple, expToTuple, updateFromTuple);
					updatePairTypes(consExp, pair, expToPair, updateFromPair);
					updateMapTypes(consExp, map, expToMap, updateFromMap);
					updateJsonTypes(consExp, json, expToJson, updateFromJson);
				}
			}
			if (updateFromTuple.size() > 0) {
				Set<Integer> tupleKeys = updateFromTuple.keySet();
				Integer tupleKey = tupleKeys.iterator().next();
				Map<Integer, Expression> tupleValue = updateFromTuple.get(tupleKey);
				updateFromTuple.remove(tupleKey);
				for (int i : tupleValue.keySet()) {
					Expression tupleExp = tupleValue.get(i);
					updateResourceTypes(tupleExp, resources, expToResource, updateFromResource);
					updateVaribleTypes(tupleExp, variables, expToVariable, updateFromVariable);
					updateMessageTypes(tupleExp, messages, expToMessage, updateFromMessage);
					updateConsOrSetTypes(tupleExp, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(tupleExp, tuple, expToTuple, updateFromTuple);
					updatePairTypes(tupleExp, pair, expToPair, updateFromPair);
					updateMapTypes(tupleExp, map, expToMap, updateFromMap);
					updateJsonTypes(tupleExp, json, expToJson, updateFromJson);
				}
			}
			if (updateFromPair.size() > 0) {
				Set<Integer> pairKeys = updateFromPair.keySet();
				Integer pairKey = pairKeys.iterator().next();
				Map<Integer, Expression> pairValue = updateFromPair.get(pairKey);
				updateFromPair.remove(pairKey);
				for (int i : pairValue.keySet()) {
					Expression pairExp = pairValue.get(i);
					updateResourceTypes(pairExp, resources, expToResource, updateFromResource);
					updateVaribleTypes(pairExp, variables, expToVariable, updateFromVariable);
					updateMessageTypes(pairExp, messages, expToMessage, updateFromMessage);
					updateConsOrSetTypes(pairExp, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(pairExp, tuple, expToTuple, updateFromTuple);
					updatePairTypes(pairExp, pair, expToPair, updateFromPair);
					updateMapTypes(pairExp, map, expToMap, updateFromMap);
					updateJsonTypes(pairExp, json, expToJson, updateFromJson);
				}
			}
			if (updateFromMap.size() > 0) {
				Set<Integer> mapKeys = updateFromMap.keySet();
				Integer mapKey = mapKeys.iterator().next();
				Map<Integer, Expression> mapValue = updateFromMap.get(mapKey);
				updateFromMap.remove(mapKey);
				for (int i : mapValue.keySet()) {
					Expression mapExp = mapValue.get(i);
					updateResourceTypes(mapExp, resources, expToResource, updateFromResource);
					updateVaribleTypes(mapExp, variables, expToVariable, updateFromVariable);
					updateMessageTypes(mapExp, messages, expToMessage, updateFromMessage);
					updateConsOrSetTypes(mapExp, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(mapExp, tuple, expToTuple, updateFromTuple);
					updatePairTypes(mapExp, pair, expToPair, updateFromPair);
					updateMapTypes(mapExp, map, expToMap, updateFromMap);
					updateJsonTypes(mapExp, json, expToJson, updateFromJson);
				}
			}
			if (updateFromJson.size() > 0) {
				Set<Integer> jsonKeys = updateFromJson.keySet();
				Integer jsonKey = jsonKeys.iterator().next();
				Map<Integer, Expression> jsonValue = updateFromJson.get(jsonKey);
				updateFromJson.remove(jsonKey);
				for (int i : jsonValue.keySet()) {
					Expression jsonExp = jsonValue.get(i);
					updateResourceTypes(jsonExp, resources, expToResource, updateFromResource);
					updateVaribleTypes(jsonExp, variables, expToVariable, updateFromVariable);
					updateMessageTypes(jsonExp, messages, expToMessage, updateFromMessage);
					updateConsOrSetTypes(jsonExp, consOrSet, expToConsOrSet, updateFromConsOrSet);
					updateTupleTypes(jsonExp, tuple, expToTuple, updateFromTuple);
					updatePairTypes(jsonExp, pair, expToPair, updateFromPair);
					updateMapTypes(jsonExp, map, expToMap, updateFromMap);
					updateJsonTypes(jsonExp, json, expToJson, updateFromJson);
				}
			}
		}
	}

	private static void updateExpressionBelonging(Map<Integer, Set<List<Expression>>> belonging, Expression exp, List<Expression> group) {
		Set<List<Expression>> groups = belonging.get(System.identityHashCode(exp));
		if (groups == null) {
			groups = new HashSet<>();
			belonging.put(System.identityHashCode(exp), groups);
			groups.add(group);
			return;
		}
		if (!groups.contains(group)) {
			groups.add(group);
		}
	}

	private static void updateResourceTypes(Expression exp, Map<ResourcePath, List<Expression>> resources,
			Map<Integer, List<Expression>> expToResource, Map<Integer, Map<Integer, Expression>> updateFromResource) {
		List<Expression> sameResource = expToResource.get(System.identityHashCode(exp));
		if (sameResource == null) return;
		for (ResourcePath res : resources.keySet()) {
			if (resources.get(res) == sameResource) {
				Type resType = res.getResourceStateType();
				Type newResType = getExpTypeIfUpdatable(resType, exp);
				if (newResType != null) {
					res.setResourceStateType(newResType);
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromResource, sameResource);
					for (Expression resExp : sameResource) {
						if (resExp != exp) {
							if (resExp instanceof Variable) {
								((Variable) resExp).setType(newResType);
								updateExps.put(System.identityHashCode(resExp), resExp);
							} else if (resExp instanceof Term) {
								((Term) resExp).setType(newResType);
								updateExps.put(System.identityHashCode(resExp), resExp);
							}
						}
					}
				}
			}
		}
	}

	private static void updateVaribleTypes(Expression exp, Map<Integer, Type> variables,
			Map<Integer, List<Expression>> expToVariable, Map<Integer, Map<Integer, Expression>> updateFromVariable) {
		List<Expression> sameVariable = expToVariable.get(System.identityHashCode(exp));
		if (sameVariable == null) return;
		Type varType = variables.get(System.identityHashCode(sameVariable));
		Type newVarType = getExpTypeIfUpdatable(varType, exp);
		if (newVarType != null) {
			variables.put(System.identityHashCode(sameVariable), newVarType);
			Map<Integer, Expression> updateVars = getUpdateSet(updateFromVariable, sameVariable);
			for (Expression v : sameVariable) {
				if (v != exp) {
					if (v instanceof Variable) {
						((Variable) v).setType(newVarType);
						updateVars.put(System.identityHashCode(v), v);
					} else if (v instanceof Term) {
						((Term) v).setType(newVarType);
						updateVars.put(System.identityHashCode(v), v);
					}
				}
			}
		} else {
			Map<Integer, Expression> updateVars = getUpdateSet(updateFromVariable, sameVariable);
			for (Expression v : sameVariable) {
				if (v instanceof Variable) {
					Type orgVarType = ((Variable) v).getType();
					if (orgVarType != varType && compareTypes(orgVarType, varType)) {
						((Variable) v).setType(varType);
						updateVars.put(System.identityHashCode(v), v);
					}
				} else if (v instanceof Term) {
					Type orgVarType = ((Term) v).getType();
					if (orgVarType != varType && compareTypes(orgVarType, varType)) {
						((Term) v).setType(varType);
						updateVars.put(System.identityHashCode(v), v);
					}
				}
			}
		}
	}

	private static void updateMessageTypes(Expression exp,
			Map<Channel, Map<Integer, Map.Entry<List<Expression>, Type>>> messages,
			Map<Integer, List<Expression>> expToMessage, Map<Integer, Map<Integer, Expression>> updateFromMessage) {
		List<Expression> messageExps = expToMessage.get(System.identityHashCode(exp));
		if (messageExps == null) return;
		Type msgType = null;
		Map.Entry<List<Expression>, Type> expsAndType = null;
		for (Channel c : messages.keySet()) {
			for (int i : messages.get(c).keySet()) {
				expsAndType = messages.get(c).get(i);
				if (expsAndType.getKey() == messageExps) {
					msgType = expsAndType.getValue();
					break;
				}
			}
			if (msgType != null) break;
		}
		if (msgType == null) return;
		Type newMsgType = getExpTypeIfUpdatable(msgType, exp);
		if (newMsgType != null) {
			expsAndType.setValue(newMsgType);
			Map<Integer, Expression> updateExps = getUpdateSet(updateFromMessage, messageExps);
			for (Expression e : messageExps) {
				if (e != exp) {
					if (e instanceof Variable) {
						((Variable) e).setType(newMsgType);
						updateExps.put(System.identityHashCode(e), e);
					} else if (e instanceof Term) {
						((Term) e).setType(newMsgType);
						updateExps.put(System.identityHashCode(e), e);
					}
				}
			}
		}
	}

	private static void updateConsOrSetTypes(Expression exp, Map<Integer, Type> consOrSet,
			Map<Integer, Set<List<Expression>>> expToConsOrSet, Map<Integer, Map<Integer, Expression>> updateFromConsOrSet) {
		Set<List<Expression>> consComponentGroups = expToConsOrSet.get(System.identityHashCode(exp));
		if (consComponentGroups == null) return;
		for (List<Expression> consOrSetComponentGroup: consComponentGroups) {
			int idx = consOrSetComponentGroup.indexOf(exp);
			switch (idx) {
			case 0:
				// exp is a list itself.
				if (!(exp instanceof Term)) break;
				Type listType = consOrSet.get(System.identityHashCode(consOrSetComponentGroup));
				Type expType = getExpTypeIfUpdatable(listType, exp);
				if (expType != null) {
					consOrSet.put(System.identityHashCode(consOrSetComponentGroup), expType);
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromConsOrSet, consOrSetComponentGroup);
					if (consOrSetComponentGroup.get(2) instanceof Variable) {
						((Variable) consOrSetComponentGroup.get(2)).setType(expType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(2)), consOrSetComponentGroup.get(2));
					} else if (consOrSetComponentGroup.get(2) instanceof Term) {
						((Term) consOrSetComponentGroup.get(2)).setType(expType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(2)), consOrSetComponentGroup.get(2));
					}
					Type compType = listComponentTypes.get(expType);
					if (consOrSetComponentGroup.get(1) != null && consOrSetComponentGroup.get(1) instanceof Variable) {
						((Variable) consOrSetComponentGroup.get(1)).setType(compType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(1)), consOrSetComponentGroup.get(1));
					} else if (consOrSetComponentGroup.get(1) != null && consOrSetComponentGroup.get(1) instanceof Term) {
						((Term) consOrSetComponentGroup.get(1)).setType(compType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(1)), consOrSetComponentGroup.get(1));
					}
				}
				break;
			case 1:
				// exp is a list's component.
				listType = consOrSet.get(System.identityHashCode(consOrSetComponentGroup));
				Type compType = listComponentTypes.get(listType);
				Type newCompType = getExpTypeIfUpdatable(compType, exp);
				if (newCompType != null) {
					Type newListType = listTypes.get(newCompType);
					if (newListType == null) {
						// Create new list type.
						newListType = createNewListType(newCompType, listType);
					}
					consOrSet.put(System.identityHashCode(consOrSetComponentGroup), newListType);
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromConsOrSet, consOrSetComponentGroup);
					if (consOrSetComponentGroup.get(0) instanceof Term) {
						((Term) consOrSetComponentGroup.get(0)).setType(newListType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(0)), consOrSetComponentGroup.get(0));
					}
					if (consOrSetComponentGroup.get(2) instanceof Variable) {
						((Variable) consOrSetComponentGroup.get(2)).setType(newListType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(2)), consOrSetComponentGroup.get(2));
					} else if (consOrSetComponentGroup.get(2) instanceof Term) {
						((Term) consOrSetComponentGroup.get(2)).setType(newListType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(2)), consOrSetComponentGroup.get(2));
					}
				}
				break;
			case 2:
				// exp is a list itself.
				listType = consOrSet.get(System.identityHashCode(consOrSetComponentGroup));
				expType = getExpTypeIfUpdatable(listType, exp);
				if (expType != null) {
					consOrSet.put(System.identityHashCode(consOrSetComponentGroup), expType);
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromConsOrSet, consOrSetComponentGroup);
					if (consOrSetComponentGroup.get(0) instanceof Term) {
						((Term) consOrSetComponentGroup.get(0)).setType(expType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(0)), consOrSetComponentGroup.get(0));
					}
					compType = listComponentTypes.get(expType);
					if (consOrSetComponentGroup.get(1) != null && consOrSetComponentGroup.get(1) instanceof Variable) {
						((Variable) consOrSetComponentGroup.get(1)).setType(compType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(1)), consOrSetComponentGroup.get(1));
					} else if (consOrSetComponentGroup.get(1) != null && consOrSetComponentGroup.get(1) instanceof Term) {
						((Term) consOrSetComponentGroup.get(1)).setType(compType);
						updateExps.put(System.identityHashCode(consOrSetComponentGroup.get(1)), consOrSetComponentGroup.get(1));
					}
				}
			}
		}
	}

	private static void updateTupleTypes(Expression exp, Map<Integer, Type> tuple,
			Map<Integer, Set<List<Expression>>> expToTuple, Map<Integer, Map<Integer, Expression>> updateFromTuple) {
		Set<List<Expression>> tupleComponentGroups = expToTuple.get(System.identityHashCode(exp));
		if (tupleComponentGroups == null) return;
		for (List<Expression> tupleComponentGroup: tupleComponentGroups) {
			int idx = tupleComponentGroup.indexOf(exp);
			if (idx == 0) {
				// exp is a tuple itself.
				Type tupleType = tuple.get(System.identityHashCode(tupleComponentGroup));
				Type newTupleType = getExpTypeIfUpdatable(tupleType, exp);
				if (newTupleType != null) {
					// Propagate an update of a tuple's type to its components' types.
					tuple.put(System.identityHashCode(tupleComponentGroup), newTupleType);
					List<Type> componentTypes = tupleComponentTypes.get(newTupleType);
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromTuple, tupleComponentGroup);
					for (int i = 1; i < tupleComponentGroup.size(); i++) {
						Expression compExp = tupleComponentGroup.get(i);
						if (compExp instanceof Variable) {
							Type compType = ((Variable) compExp).getType();
							if (compType != null && DataConstraintModel.typeTuple.isAncestorOf(compType)) {
								// If the type of one component (compExp) is also tuple.
								Type newExpType = tupleTypes.get(componentTypes.subList(i - 1, componentTypes.size()));
								if (newExpType == null) {
									// Create new tuple type;
									newExpType = createNewTupleType(componentTypes.subList(i - 1, componentTypes.size()), compType);
								}
								if (compareTypes(compType, newExpType)) {
									((Variable) compExp).setType(newExpType);
									updateExps.put(System.identityHashCode(compExp), compExp);
								}
							} else {
								if (i - 1 < componentTypes.size()) {
									if (compareTypes(compType, componentTypes.get(i - 1))) {
										((Variable) compExp).setType(componentTypes.get(i - 1));
										updateExps.put(System.identityHashCode(compExp), compExp);
									}									
								} else {
									// for insert
									if (compareTypes(compType, newTupleType)) {
										((Variable) compExp).setType(newTupleType);
										updateExps.put(System.identityHashCode(compExp), compExp);
									}									
								}
							}
						} else if (compExp instanceof Term) {
							Type compType = ((Term) compExp).getType();
							if (compType != null && DataConstraintModel.typeTuple.isAncestorOf(compType)) {
								// If the type of one component (compExp) is also tuple.
								Type newExpType = tupleTypes.get(componentTypes.subList(i - 1, componentTypes.size()));
								if (newExpType == null) {
									// Create new tuple type;
									newExpType = createNewTupleType(componentTypes.subList(i - 1, componentTypes.size()), compType);
								}
								if (compareTypes(compType, newExpType)) {
									((Term) compExp).setType(newExpType);
									updateExps.put(System.identityHashCode(compExp), compExp);
								}
							} else {
								if (i - 1 < componentTypes.size()) {
									if (compareTypes(compType, componentTypes.get(i - 1))) {
										((Term) compExp).setType(componentTypes.get(i - 1));
										updateExps.put(System.identityHashCode(compExp), compExp);
									}
								} else {
									// for insert
									if (compareTypes(compType, newTupleType)) {
										((Term) compExp).setType(newTupleType);
										updateExps.put(System.identityHashCode(compExp), compExp);
									}
								}
							}
						}
					}
				}
			} else {
				// exp is a tuple's component.
				Type tupleType = tuple.get(System.identityHashCode(tupleComponentGroup));
				List<Type> componentTypes = tupleComponentTypes.get(tupleType);
				boolean updated = false;
				if (idx == 1) {
					Type compType = componentTypes.get(idx - 1);
					Type newCompType = getExpTypeIfUpdatable(compType, exp);
					if (newCompType != null) {
						componentTypes = new ArrayList<>(componentTypes);
						componentTypes.set(idx - 1, newCompType);
						updated = true;
					}
				} else {
					Type expType = null;
					if (exp instanceof Term) {
						expType = ((Term) exp).getType();
					} else if (exp instanceof Variable) {
						expType = ((Variable) exp).getType();
					}
					if (expType != null && DataConstraintModel.typeTuple.isAncestorOf(expType)) {
						// If the type of the updated component (exp) is also tuple.
						List<Type> subCompTypes = tupleComponentTypes.get(expType);
						componentTypes = new ArrayList<>(componentTypes);
						for (int i = 0; i < subCompTypes.size(); i++) {
							if (componentTypes.size() < i + 2) {
								componentTypes.add(subCompTypes.get(i));
								updated = true;
							} else if (compareTypes(componentTypes.get(i + 1), subCompTypes.get(i))) {
								componentTypes.set(i + 1, subCompTypes.get(i));
								updated = true;
							}
						}
					} else {
						Type compType = componentTypes.get(idx - 1);
						Type newCompType = getExpTypeIfUpdatable(compType, exp);
						if (newCompType != null) {
							componentTypes = new ArrayList<>(componentTypes);
							componentTypes.set(idx - 1, newCompType);
							updated = true;
						}
					}
				}
				if (updated) {
					// Propagate an update of a component's type to its container's (tuple's) type.
					Type newTupleType = tupleTypes.get(componentTypes);
					if (newTupleType == null) {
						// Create new tuple type;
						newTupleType = createNewTupleType(componentTypes, tupleType);
					}
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromTuple, tupleComponentGroup);
					Expression tupleExp = tupleComponentGroup.get(0);
					if (tupleExp instanceof Variable) {
						((Variable) tupleExp).setType(newTupleType);
						updateExps.put(System.identityHashCode(tupleExp), tupleExp);
					} else if (tupleExp instanceof Term) {
						((Term) tupleExp).setType(newTupleType);
						updateExps.put(System.identityHashCode(tupleExp), tupleExp);
					}
					tuple.put(System.identityHashCode(tupleComponentGroup), newTupleType);
				}
			}
		}
	}

	private static void updatePairTypes(Expression exp, Map<Integer, Type> pair,
			Map<Integer, Set<List<Expression>>> expToPair, Map<Integer, Map<Integer, Expression>> updateFromPair) {
		Set<List<Expression>> pairComponentGroups = expToPair.get(System.identityHashCode(exp));
		if (pairComponentGroups == null) return;
		for (List<Expression> pairComponentGroup: pairComponentGroups) {
			int idx = pairComponentGroup.indexOf(exp);
			if (idx == 0) {
				// exp is a pair itself.
				Type pairType = pair.get(System.identityHashCode(pairComponentGroup));
				Type newPairType = getExpTypeIfUpdatable(pairType, exp);
				if (newPairType != null) {
					// Propagate an update of a pair's type to its components' types.
					pair.put(System.identityHashCode(pairComponentGroup), newPairType);
					Type componentType = pairComponentTypes.get(newPairType);
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromPair, pairComponentGroup);
					for (int i = 1; i < pairComponentGroup.size(); i++) {
						Expression compExp = pairComponentGroup.get(i);
						if (compExp instanceof Variable) {
							if (compareTypes(((Variable) compExp).getType(), componentType)) {
								((Variable) compExp).setType(componentType);
								updateExps.put(System.identityHashCode(compExp), compExp);
							}
						} else if (compExp instanceof Term) {
							if (compareTypes(((Term) compExp).getType(), componentType)) {
								((Term) compExp).setType(componentType);
								updateExps.put(System.identityHashCode(compExp), compExp);
							}
						}
					}
				}
			} else {
				// exp is a pair's component.
				Type pairType = pair.get(System.identityHashCode(pairComponentGroup));
				Type compType = pairComponentTypes.get(pairType);
				Type newCompType = getExpTypeIfUpdatable(compType, exp);
				if (newCompType != null) {
					// Propagate an update of a component's type to its container's (pair's) type.
					Type newPairType = pairTypes.get(compType);
					if (newPairType != null) {
						Map<Integer, Expression> updateExps = getUpdateSet(updateFromPair, pairComponentGroup);
						Expression pairExp = pairComponentGroup.get(0);
						if (pairExp instanceof Variable) {
							((Variable) pairExp).setType(newPairType);
							updateExps.put(System.identityHashCode(pairExp), pairExp);
						} else if (pairExp instanceof Term) {
							((Term) pairExp).setType(newPairType);
							updateExps.put(System.identityHashCode(pairExp), pairExp);
						}
						pair.put(System.identityHashCode(pairComponentGroup), newPairType);
					}
				}
			}
		}
	}
	
	private static void updateMapTypes(Expression exp, Map<Integer, Type> map,
			Map<Integer, Set<List<Expression>>> expToMap, Map<Integer, Map<Integer, Expression>> updateFromMap) {
		Set<List<Expression>> mapComponentGroups = expToMap.get(System.identityHashCode(exp));
		if (mapComponentGroups == null) return;
		for (List<Expression> mapComponentGroup: mapComponentGroups) {
			int idx = mapComponentGroup.indexOf(exp);
			if (idx == 0 || idx == 3) {
				// exp is a map itself.
				Type mapType = map.get(System.identityHashCode(mapComponentGroup));
				Type newMapType = getExpTypeIfUpdatable(mapType, exp);
				if (newMapType != null) {
					// Propagate an update of a map's type to its components' types.
					map.put(System.identityHashCode(mapComponentGroup), newMapType);
					List<Type> componentTypes = mapComponentTypes.get(newMapType);
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromMap, mapComponentGroup);
					for (int i = 1; i < mapComponentGroup.size() && i < 3; i++) {
						Expression compExp = mapComponentGroup.get(i);
						if (compExp instanceof Variable) {
							if (compareTypes(((Variable) compExp).getType(), componentTypes.get(i - 1))) {
								((Variable) compExp).setType(componentTypes.get(i - 1));
								updateExps.put(System.identityHashCode(compExp), compExp);
							}
						} else if (compExp instanceof Term) {
							if (compareTypes(((Term) compExp).getType(), componentTypes.get(i - 1))) {
								((Term) compExp).setType(componentTypes.get(i - 1));
								updateExps.put(System.identityHashCode(compExp), compExp);
							}
						}
					}
					// Propagate an update of a map's type to another map's type.
					Expression compExp = null;
					if (idx == 0 && mapComponentGroup.size() == 4) {		// for insert
						compExp = mapComponentGroup.get(3);
					} else if (idx == 3) {
						compExp = mapComponentGroup.get(0);					
					}
					if (compExp != null) {
						if (compExp instanceof Variable) {
							if (compareTypes(((Variable) compExp).getType(), newMapType)) {
								((Variable) compExp).setType(newMapType);
								updateExps.put(System.identityHashCode(compExp), compExp);
							}
						} else if (compExp instanceof Term) {
							if (compareTypes(((Term) compExp).getType(), newMapType)) {
								((Term) compExp).setType(newMapType);
								updateExps.put(System.identityHashCode(compExp), compExp);
							}
						}
					}
				}
			} else {
				// exp is a map's key or value.
				Type mapType = map.get(System.identityHashCode(mapComponentGroup));
				List<Type> componentTypes = mapComponentTypes.get(mapType);
				Type compType = componentTypes.get(idx - 1);
				Type newCompType = getExpTypeIfUpdatable(compType, exp);
				if (newCompType != null) {
					// Propagate an update of a component's type to its container's (map's) type.
					componentTypes = new ArrayList<>(componentTypes);
					componentTypes.set(idx - 1, newCompType);
					Type newMapType = mapTypes.get(componentTypes);
					if (newMapType == null) {
						// Create new map type;
						newMapType = createNewMapType(componentTypes, mapType);
					}
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromMap, mapComponentGroup);
					Expression mapExp = mapComponentGroup.get(0);
					if (mapExp instanceof Variable) {
						((Variable) mapExp).setType(newMapType);
						updateExps.put(System.identityHashCode(mapExp), mapExp);
					} else if (mapExp instanceof Term) {
						((Term) mapExp).setType(newMapType);
						updateExps.put(System.identityHashCode(mapExp), mapExp);
					}
					if (mapComponentGroup.size() == 4) {	// for insert
						mapExp = mapComponentGroup.get(3);
						if (mapExp instanceof Variable) {
							((Variable) mapExp).setType(newMapType);
							updateExps.put(System.identityHashCode(mapExp), mapExp);
						} else if (mapExp instanceof Term) {
							((Term) mapExp).setType(newMapType);
							updateExps.put(System.identityHashCode(mapExp), mapExp);
						}					
					}
					map.put(System.identityHashCode(mapComponentGroup), newMapType);
				}
			}
		}
	}
	
	
	private static void updateJsonTypes(Expression exp, Map<Integer, Type> json,
			Map<Integer, Set<List<Expression>>> expToJson, Map<Integer, Map<Integer, Expression>> updateFromJson) {
		Set<List<Expression>> jsonMemberGroups = expToJson.get(System.identityHashCode(exp));
		if (jsonMemberGroups == null) return;
		for (List<Expression> jsonMemberGroup: jsonMemberGroups) {
			int idx = jsonMemberGroup.indexOf(exp);
			if (idx == 3) {
				// exp is a json argument (0:t = addMember(3:json, 1:key, 2:value)).
				Type jsonType = json.get(System.identityHashCode(jsonMemberGroup));
				Map<String, Type> memberTypes = new HashMap<>(jsonMemberTypes.get(jsonType));
				Map<String, Type> argMemberTypes = new HashMap<>(memberTypes);
				String keyName = null;
				Type valueType = null;
				if (jsonMemberGroup.get(1) instanceof Constant) {
					keyName = ((Constant) jsonMemberGroup.get(1)).getSymbol().getName();
					valueType = ((Constant) jsonMemberGroup.get(1)).getType();
					argMemberTypes.remove(keyName);
				}
				Type jsonArgType = jsonTypes.get(argMemberTypes);
				Type newJsonArgType = getExpTypeIfUpdatable(jsonArgType, exp);
				if (newJsonArgType != null && keyName != null) {
					// Propagate an update of a json arg's type to its container's (json's) type.
					argMemberTypes = ((JsonType) newJsonArgType).getMemberTypes();
					argMemberTypes.put(keyName, valueType);
					memberTypes.putAll(argMemberTypes);
					Type newJsonType = jsonTypes.get(memberTypes);
					if (newJsonType == null) {
						// Create new json type.
						newJsonType = createNewJsonType(memberTypes, jsonType);
					}
					// Update the type of the json term and record the updated expression.
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromJson, jsonMemberGroup);
					Expression jsonExp = jsonMemberGroup.get(0);
					if (jsonExp instanceof Variable) {
						((Variable) jsonExp).setType(newJsonType);
						updateExps.put(System.identityHashCode(jsonExp), jsonExp);
					} else if (jsonExp instanceof Term) {
						((Term) jsonExp).setType(newJsonType);
						updateExps.put(System.identityHashCode(jsonExp), jsonExp);
					}
					json.put(System.identityHashCode(jsonMemberGroup), newJsonType);
				}
			} else if (idx == 2) {
				// exp is a value (0:t = addMember(3:json, 1:key, 2:value) or 2:value = dot(0:list/map, 1:key)).
				Type jsonType = json.get(System.identityHashCode(jsonMemberGroup));
				Type newJsonType = null;
				if (exp instanceof Term && ((Term) exp).getSymbol().equals(DataConstraintModel.dotParam)) {
					if (DataConstraintModel.typeList.isAncestorOf(jsonType)) {
						Type elementType = listComponentTypes.get(jsonType);
						Type newElementType = getExpTypeIfUpdatable(elementType, exp);
						if (newElementType != null) {
							// Propagate an update of a member's type to its container's (json's) type.
							newJsonType = listTypes.get(newElementType);
							if (newJsonType == null) {
								// Create new json type.
								newJsonType = createNewListType(newElementType, jsonType);
							}
						}
					} else if (DataConstraintModel.typeMap.isAncestorOf(jsonType)) {
						List<Type> keyValueTypes = mapComponentTypes.get(jsonType);
						Type newValueType = getExpTypeIfUpdatable(keyValueTypes.get(1), exp);
						if (newValueType != null) {
							// Propagate an update of a member's type to its container's (json's) type.
							List<Type> newKeyValueTypes = Arrays.asList(new Type[] {DataConstraintModel.typeString, newValueType});
							newJsonType = mapTypes.get(newKeyValueTypes);
							if (newJsonType == null) {
								// Create new json type.
								newJsonType = createNewMapType(newKeyValueTypes, jsonType);
							}
						}
					}
				} else {
					Map<String, Type> memberTypes = jsonMemberTypes.get(jsonType);
					String keyName = null;
					if (jsonMemberGroup.get(1) instanceof Constant) {
						keyName = ((Constant) jsonMemberGroup.get(1)).getSymbol().getName();
					}
					if (memberTypes != null) {
						Type memberType = memberTypes.get(keyName);
						Type newMemberType = getExpTypeIfUpdatable(memberType, exp);
						if (newMemberType != null && keyName != null) {
							// Propagate an update of a member's type to its container's (json's) type.
							Map<String, Type> newMemberTypes = new HashMap<>(memberTypes);
							newMemberTypes.put(keyName, newMemberType);
							newJsonType = jsonTypes.get(newMemberTypes);
							if (newJsonType == null) {
								// Create new json type.
								newJsonType = createNewJsonType(newMemberTypes, jsonType);
							}
						}
					}
				}
				if (newJsonType != null) {
					// Update the type of the json term and record the updated expression.
					Map<Integer, Expression> updateExps = getUpdateSet(updateFromJson, jsonMemberGroup);
					Expression jsonExp = jsonMemberGroup.get(0);
					if (jsonExp instanceof Variable) {
						((Variable) jsonExp).setType(newJsonType);
						updateExps.put(System.identityHashCode(jsonExp), jsonExp);
					} else if (jsonExp instanceof Term) {
						((Term) jsonExp).setType(newJsonType);
						updateExps.put(System.identityHashCode(jsonExp), jsonExp);
					}
					json.put(System.identityHashCode(jsonMemberGroup), newJsonType);
				}
			}
		}
	}
	
	private static Type createNewListType(Type compType, Type parentType) {
		String compTypeName = getInterfaceTypeName(compType);
		List<Type> childrenTypes = getChildrenTypes(parentType, listComponentTypes.keySet());
		Type newListType = new Type("List", "ArrayList<>", "List<" + compTypeName + ">", parentType);
		listTypes.put(compType, newListType);
		listComponentTypes.put(newListType, compType);
		for (Type childType : childrenTypes) {
			if (compareTypes(childType, newListType)) {
				if (newListType.getParentTypes().contains(parentType)) {
					newListType.replaceParentType(parentType, childType);
				} else {
					newListType.addParentType(childType);
				}
			} else if (compareTypes(newListType, childType)) {
				childType.replaceParentType(parentType, newListType);
			}
		}
		return newListType;
	}

	private static Type createNewTupleType(List<Type> componentTypes, Type parentTupleType) {
		String implTypeName = "AbstractMap.SimpleEntry<>";
		String interfaceTypeName = "Map.Entry<$x>";
		if (componentTypes.size() >= 2) {
			implTypeName = implTypeName.replace("$x", getImplementationTypeName(componentTypes.get(0)) + "$x");
			interfaceTypeName = interfaceTypeName.replace("$x", getInterfaceTypeName(componentTypes.get(0)) + "$x");
			for (Type argType : componentTypes.subList(1, componentTypes.size() - 1)) {
				implTypeName = implTypeName.replace("$x",
						", AbstractMap.SimpleEntry<" + getImplementationTypeName(argType) + "$x>");
				interfaceTypeName = interfaceTypeName.replace("$x",
						", Map.Entry<" + getInterfaceTypeName(argType) + "$x>");
			}
			implTypeName = implTypeName.replace("$x",
					", " + getImplementationTypeName(componentTypes.get(componentTypes.size() - 1)));
			interfaceTypeName = interfaceTypeName.replace("$x",
					", " + getInterfaceTypeName(componentTypes.get(componentTypes.size() - 1)));
		}
		List<Type> childrenTypes = getChildrenTypes(parentTupleType, tupleComponentTypes.keySet());
		Type newTupleType = new Type("Tuple", implTypeName, interfaceTypeName, parentTupleType);
		tupleTypes.put(componentTypes, newTupleType);
		tupleComponentTypes.put(newTupleType, componentTypes);
		for (Type childType : childrenTypes) {
			if (compareTypes(childType, newTupleType)) {
				if (newTupleType.getParentTypes().contains(parentTupleType)) {
					newTupleType.replaceParentType(parentTupleType, childType);
				} else {
					newTupleType.addParentType(childType);
				}
			} else if (compareTypes(newTupleType, childType)) {
				childType.replaceParentType(parentTupleType, newTupleType);
			}
		}
		return newTupleType;
	}

	private static Type createNewMapType(List<Type> componentTypes, Type parentMapType) {
		String implTypeName = "HashMap<>";
		String interfaceTypeName = "Map<$x, $y>";
		if (componentTypes.size() == 2) {
			implTypeName = implTypeName.replace("$x", getImplementationTypeName(componentTypes.get(0)));
			interfaceTypeName = interfaceTypeName.replace("$x", getInterfaceTypeName(componentTypes.get(0)));
			implTypeName = implTypeName.replace("$y", getImplementationTypeName(componentTypes.get(1)));
			interfaceTypeName = interfaceTypeName.replace("$y", getInterfaceTypeName(componentTypes.get(1)));
		}
		List<Type> childrenTypes = getChildrenTypes(parentMapType, mapComponentTypes.keySet());
		Type newMapType = new Type("Map", implTypeName, interfaceTypeName, parentMapType);
		mapTypes.put(componentTypes, newMapType);
		mapComponentTypes.put(newMapType, componentTypes);
		for (Type childType : childrenTypes) {
			if (compareTypes(childType, newMapType)) {
				if (newMapType.getParentTypes().contains(parentMapType)) {
					newMapType.replaceParentType(parentMapType, childType);
				} else {
					newMapType.addParentType(childType);
				}
			} else if (compareTypes(newMapType, childType)) {
				childType.replaceParentType(parentMapType, newMapType);
			}
		}
		return newMapType;
	}

	private static JsonType createNewJsonType(Map<String, Type> memberTypes, Type parentJsonType) {
		String implTypeName = "HashMap<>";
		String interfaceTypeName = "Map<String, Object>";
		List<Type> childrenTypes = getChildrenTypes(parentJsonType, jsonMemberTypes.keySet());
		JsonType newJsonType = new JsonType("Json", implTypeName, interfaceTypeName, parentJsonType);
		for (String key: memberTypes.keySet()) {
			newJsonType.addMemberType(key, memberTypes.get(key));
		}
		jsonTypes.put(memberTypes, newJsonType);
		jsonMemberTypes.put(newJsonType, memberTypes);
		for (Type childType : childrenTypes) {
			if (compareTypes(childType, newJsonType)) {
				if (newJsonType.getParentTypes().contains(parentJsonType)) {
					newJsonType.replaceParentType(parentJsonType, childType);
				} else {
					newJsonType.addParentType(childType);
				}
			} else if (compareTypes(newJsonType, childType)) {
				childType.replaceParentType(parentJsonType, newJsonType);
			}
		}
		return newJsonType;
	}

	/**
	 * Get children types of a given type from given set of types.
	 * @param parentType a type
	 * @param allTypes set of types
	 * @return list of the children types
	 */
	private static List<Type> getChildrenTypes(Type parentType, Set<Type> allTypes) {
		List<Type> childrenTypes = new ArrayList<>();
		for (Type childType : allTypes) {
			if (childType.getParentTypes().contains(parentType)) {
				childrenTypes.add(childType);
			}
		}
		return childrenTypes;
	}

	private static String getImplementationTypeName(Type type) {
		if (type == null)
			return "Object";
		String wrapperType = DataConstraintModel.getWrapperType(type);
		if (wrapperType != null)
			return wrapperType;
		return type.getImplementationTypeName();
	}

	private static String getInterfaceTypeName(Type type) {
		if (type == null)
			return "Object";
		String wrapperType = DataConstraintModel.getWrapperType(type);
		if (wrapperType != null)
			return wrapperType;
		return type.getInterfaceTypeName();
	}

	private static <T extends Expression, U extends Collection<T>> Map<Integer, T> getUpdateSet(Map<Integer, Map<Integer, T>> updateSets, U keySet) {
		Map<Integer, T> updatedExps = updateSets.get(System.identityHashCode(keySet));
		if (updatedExps == null) {
			updatedExps = new HashMap<>();
			updateSets.put(System.identityHashCode(keySet), updatedExps);
		}
		return updatedExps;
	}

	private static Type getExpTypeIfUpdatable(Type originalType, Expression newExp) {
		Type expType = null;
		if (newExp instanceof Term) {
			expType = ((Term) newExp).getType();
		} else if (newExp instanceof Variable) {
			expType = ((Variable) newExp).getType();
		}
		if (compareTypes(originalType, expType)) {
			return expType;
		}
		return null;
	}

	/**
	 * Is an given original type an ancestor of a given new type?
	 * 
	 * @param originalType original type
	 * @param newType      new type (may not have been registered)
	 * @return true: if the original type equals to the new type or is an ancestor
	 *         of the new type, false: otherwise
	 */
	private static boolean compareTypes(Type originalType, Type newType) {
		if (originalType == null) return true;
		if (originalType != newType && newType != null) {
			if (originalType.isAncestorOf(newType)) return true;
			if (newType.isAncestorOf(originalType)) return false;
			if (DataConstraintModel.typeMap.isAncestorOf(originalType)
					&& DataConstraintModel.typeMap.isAncestorOf(newType)) {
				List<Type> originalCompTypes = mapComponentTypes.get(originalType);
				List<Type> newCompTypes = mapComponentTypes.get(newType);
				if (originalCompTypes == null) return true;
				for (int i = 0; i < originalCompTypes.size(); i++) {
					if (originalCompTypes.get(i) != null && 
							(newCompTypes.get(i) == null || !originalCompTypes.get(i).isAncestorOf(newCompTypes.get(i)))) return false;
				}
				return true;
			}
			if (DataConstraintModel.typeTuple.isAncestorOf(originalType)
					&& DataConstraintModel.typeTuple.isAncestorOf(newType)) {
				List<Type> originalCompTypes = tupleComponentTypes.get(originalType);
				List<Type> newCompTypes = tupleComponentTypes.get(newType);
				if (originalCompTypes == null) return true;
				originalCompTypes = new ArrayList<>(originalCompTypes);
				newCompTypes = new ArrayList<>(newCompTypes);
				for (int i = 0; i < originalCompTypes.size(); i++) {
					if (originalCompTypes.get(i) != null) {
						if (DataConstraintModel.typeTuple.isAncestorOf(originalCompTypes.get(i))) {
							// Unfold the nested tuple's types.
							Type tupleType = originalCompTypes.remove(i); 
							for (Type t: tupleComponentTypes.get(tupleType)) {
								originalCompTypes.add(t);
							}
						}
						if (newCompTypes.size() - 1 < i) return false;
						if (newCompTypes.get(i) != null && DataConstraintModel.typeTuple.isAncestorOf(newCompTypes.get(i))) {
							// Unfold the nested tuple's types.
							Type tupleType = newCompTypes.remove(i); 
							for (Type t: tupleComponentTypes.get(tupleType)) {
								newCompTypes.add(t);
							}
						}
						if (newCompTypes.get(i) == null || !originalCompTypes.get(i).isAncestorOf(newCompTypes.get(i))) return false;
					}
				}
				return true;
			}
			if (DataConstraintModel.typeList.isAncestorOf(originalType)
					&& DataConstraintModel.typeList.isAncestorOf(newType)) {
				Type originalCompType = listComponentTypes.get(originalType);
				Type newCompType = listComponentTypes.get(newType);
				if (originalCompType != null && (newCompType == null || !originalCompType.isAncestorOf(newCompType))) return false;
				return true;
			}
			if (DataConstraintModel.typeJson.isAncestorOf(originalType) && DataConstraintModel.typeJson.isAncestorOf(newType)) {
				Map<String, Type> originalMemberTypes = jsonMemberTypes.get(originalType);
				Map<String, Type> newMemberTypes = jsonMemberTypes.get(newType);
				if (originalMemberTypes == null) return true;
				if (originalMemberTypes.keySet().size() < newMemberTypes.keySet().size() 
						&& newMemberTypes.keySet().containsAll(originalMemberTypes.keySet())) return true;
				if (originalMemberTypes.keySet().size() > newMemberTypes.keySet().size()) return false;
				if (!newMemberTypes.keySet().containsAll(originalMemberTypes.keySet())) return false;
				for (String key: originalMemberTypes.keySet()) {
					if (!originalMemberTypes.get(key).isAncestorOf(newMemberTypes.get(key))) return false;
				}
				return true;
			}
		}
		return false;
	}
}
