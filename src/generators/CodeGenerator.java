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
import models.Edge;
import models.Node;
import models.algebra.Expression;
import models.algebra.Field;
import models.algebra.Parameter;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Type;
import models.algebra.Variable;
import models.controlFlowModel.EntryPointObjectNode;
import models.controlFlowModel.ObjectNode;
import models.controlFlowModel.StatefulObjectNode;
import models.dataConstraintModel.Channel;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.ResourcePath;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataTransferChannel;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.IFlowGraph;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.DataTransferChannel.IResourceStateAccessor;

/**
 * Common generator for prototypes
 * 
 * @author Nitta
 *
 */
public abstract class CodeGenerator {
	public static final String fieldOfResourceState = "value";
	public static final String getterOfResourceState = "getValue";
	public static final String updateMethodName = "update";
	private static String mainTypeName = null;
		
	public static String getMainTypeName() {
		return mainTypeName;
	}

	public static void setMainTypeName(String mainTypeName) {
		CodeGenerator.mainTypeName = mainTypeName;
	}
	
	public static void resetMainTypeName() {
		CodeGenerator.mainTypeName = null;
	}

	/**
	 * Generate source codes in specified language from data-flow/control-flow graph.
	 * 
	 * @param model architecture model
	 * @param flowGraph data-flow or control-flow graph
	 * @param langSpec specified language
	 * @return source codes
	 */
	public ArrayList<CompilationUnit> generateCode(DataTransferModel model, IFlowGraph flowGraph, ILanguageSpecific langSpec) {
		ArrayList<CompilationUnit> codes = new ArrayList<>();
		
		// Sort the all components.
		ArrayList<Node> components = determineComponentOrder(flowGraph);
		
		// Add the main component.
		if (mainTypeName == null) {
			mainTypeName = langSpec.getMainComponentName();
		}
		TypeDeclaration mainComponent = langSpec.newTypeDeclaration(mainTypeName);
		MethodDeclaration mainConstructor = mainComponent.createConstructor();
		CompilationUnit mainCU = langSpec.newCompilationUnit(mainComponent);
		codes.add(mainCU);
		
		// Generate the other components.
		generateCodeFromFlowGraph(model, flowGraph, components, mainComponent, mainConstructor, codes, langSpec);
		
		return codes;
	}
	
	public abstract void generateCodeFromFlowGraph(DataTransferModel model, IFlowGraph flowGraph, ArrayList<Node> components,
			TypeDeclaration mainComponent, MethodDeclaration mainConstructor, ArrayList<CompilationUnit> codes, ILanguageSpecific langSpec);
	
	private static ArrayList<Node> determineComponentOrder(IFlowGraph graph) {
		ArrayList<Node> objects = new ArrayList<>();
		Set<Node> visited = new HashSet<>();
		Set<Node> allNodes = graph.getAllNodes();
		for (Node n: allNodes) {
			if (!(n instanceof EntryPointObjectNode)) {
				topologicalSort(allNodes, n, visited, objects);
			}
		}
		return objects;
	}
	
	private static void topologicalSort(Set<Node> allNodes, Node curNode, Set<Node> visited, List<Node> orderedList) {
		if (visited.contains(curNode)) return;
		visited.add(curNode);
		// a caller is before the callee
		for (Edge e: curNode.getInEdges()) {
			if (!(e.getSource() instanceof EntryPointObjectNode)) {
				if (!(e instanceof DataFlowEdge) || ((PushPullAttribute)((DataFlowEdge) e).getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
					topologicalSort(allNodes, e.getSource(), visited, orderedList);
				}
			}
		}
		if (curNode instanceof ResourceNode) {
			for (Edge e: curNode.getOutEdges()) {
				DataFlowEdge de = (DataFlowEdge) e;
				if (((PushPullAttribute) de.getAttribute()).getOptions().get(0) != PushPullValue.PUSH) {
					topologicalSort(allNodes, e.getDestination(), visited, orderedList);
				}
			}
		}
		// For reference resources.
		ResourceNode cn = null;
		if (curNode instanceof ResourceNode) {
			cn = (ResourceNode) curNode;
		} else if (curNode instanceof StatefulObjectNode) {
			cn = ((StatefulObjectNode) curNode).getResource();
		}
		if (cn != null) {
			for (Node n: allNodes) {
				ResourceNode rn = null;
				if (n instanceof ResourceNode) {
					rn = (ResourceNode) n;
				} else if (n instanceof StatefulObjectNode) {
					rn = ((StatefulObjectNode) n).getResource();
				}
				if (rn != null) {
					for (Edge e: rn.getOutEdges()) {
						DataTransferChannel ch = ((DataFlowEdge) e).getChannel();
						for (ChannelMember m: ch.getReferenceChannelMembers()) {
							if (m.getResource() == cn.getResource()) {
								topologicalSort(allNodes, n, visited, orderedList);
							}
						}
					}
				}
			}
		}
		orderedList.add(0, curNode);
	}

	protected void updateMainComponent(DataTransferModel model, TypeDeclaration mainType, MethodDeclaration mainConstructor, Node componentNode, 
			final List<ResourcePath> depends, ILanguageSpecific langSpec) {
		// Declare the field to refer to each object in the main type.
		ResourceNode resNode = null;
		String nodeName = null;
		if (componentNode instanceof ResourceNode) {
			resNode = (ResourceNode) componentNode;
			nodeName = resNode.getResource().getResourceName();
		} else if (componentNode instanceof ObjectNode) {
			nodeName = ((ObjectNode) componentNode).getName();
			if (componentNode instanceof StatefulObjectNode) {
				resNode = ((StatefulObjectNode) componentNode).getResource();
			}
		}
		String componentName = langSpec.toComponentName(nodeName);
		// Declare a field to refer each object.
		if (langSpec.declareField()) {
			FieldDeclaration refField = langSpec.newFieldDeclaration(new Type(componentName, componentName), nodeName);
			mainType.addField(refField);
		}
		// Add a statement to instantiate each object to the main constructor.
		List<String> parameters = new ArrayList<>();
		for (ResourcePath id: depends) {
			// For the callee objects (the destination resource of push transfer or the source resource of pull transfer).
			parameters.add(id.getResourceName());
		}
		// For the refs. 
		if (resNode != null) {
			Set<ResourcePath> refs = new HashSet<>();
			for (Channel cg : model.getChannels()) {
				DataTransferChannel ch = (DataTransferChannel) cg;
				if (ch.getInputResources().contains(resNode.getResource())) {
					for (ResourcePath id: ch.getReferenceResources()) {
						if (!refs.contains(id) && !depends.contains(id)) {
							refs.add(id);
							String refResName = id.getResourceName();
							parameters.add(refResName);
						}
					}
				}
			}
		}

		Block mainConstructorBody = mainConstructor.getBody();
		if (mainConstructorBody == null) {
			mainConstructorBody = new Block();
			mainConstructor.setBody(mainConstructorBody);
		}
		mainConstructorBody.addStatement(langSpec.getFieldAccessor(nodeName) + langSpec.getAssignment() + langSpec.getConstructorInvocation(componentName, parameters) + langSpec.getStatementDelimiter());
	}

	protected void addReference(TypeDeclaration component, MethodDeclaration constructor, Node dstNode, ILanguageSpecific langSpec) {
		String dstNodeName = null;
		if (dstNode instanceof ResourceNode) {
			dstNodeName = ((ResourceNode) dstNode).getResource().getResourceName();
		} else if (dstNode instanceof ObjectNode) {
			dstNodeName = ((ObjectNode) dstNode).getName();
		}
		String dstComponentName = langSpec.toComponentName(dstNodeName);
		if (langSpec.declareField()) {
			// Declare a field to refer to another component.
			component.addField(langSpec.newFieldDeclaration(new Type(dstComponentName, dstComponentName), dstNodeName));
		}
		// Initialize the field to refer to another component.
		constructor.addParameter(langSpec.newVariableDeclaration(new Type(dstComponentName, dstComponentName), dstNodeName));
		constructor.getBody().addStatement(langSpec.getFieldAccessor(dstNodeName) + langSpec.getAssignment() + dstNodeName + langSpec.getStatementDelimiter());
	}

	protected void fillGetterMethodToReturnStateField(MethodDeclaration getter, Type resStateType, ILanguageSpecific langSpec) {
		// returns the state field when all incoming data-flow edges are PUSH-style.
		if (langSpec.isValueType(resStateType)) {
			getter.addStatement(langSpec.getReturnStatement(langSpec.getFieldAccessor(fieldOfResourceState)) + langSpec.getStatementDelimiter());		// return value;
		} else {
			// copy the current state to be returned as a 'value'
			String implTypeName = resStateType.getImplementationTypeName();
//					String interfaceTypeName = resourceType.getInterfaceTypeName();
//					String concreteTypeName;
//					if (interfaceTypeName.contains("<")) {
//						String typeName = implTypeName.substring(0, implTypeName.indexOf("<"));
////						String generics = interfaceTypeName.substring(interfaceTypeName.indexOf("<") + 1, interfaceTypeName.lastIndexOf(">"));
//						concreteTypeName = typeName + "<>";
//					} else {
//						concreteTypeName = implTypeName;
//					}
			List<String> parameters = new ArrayList<>();
			parameters.add(langSpec.getFieldAccessor(fieldOfResourceState));
			getter.addStatement(langSpec.getReturnStatement(langSpec.getConstructorInvocation(implTypeName, parameters)) + langSpec.getStatementDelimiter());	// return new Resource(value);
		}
	}

	protected void declareAccessorInMainComponent(TypeDeclaration mainComponent, ResourcePath accessResId, ILanguageSpecific langSpec) {
		MethodDeclaration getter = new MethodDeclaration("get" + langSpec.toComponentName(accessResId.getResourceName()), accessResId.getResourceStateType());
		Block block = new Block();
		block.addStatement(langSpec.getReturnStatement(langSpec.getMethodInvocation(accessResId.getResourceName(), getterOfResourceState)) + langSpec.getStatementDelimiter());
		getter.setBody(block);
		mainComponent.addMethod(getter);
	}

	protected void declareFieldsToReferenceResources(DataTransferModel model, ResourceNode resourceNode, TypeDeclaration component, MethodDeclaration constructor,
			final List<ResourcePath> depends, ILanguageSpecific langSpec) {
		Set<ResourcePath> refs = new HashSet<>();
		for (Channel ch : model.getChannels()) {
			DataTransferChannel c = (DataTransferChannel) ch;
			if (c.getInputResources().contains(resourceNode.getResource())) {
				for (ResourcePath id: c.getReferenceResources()) {
					if (!refs.contains(id) && !depends.contains(id)) {
						refs.add(id);
						String refResName = langSpec.toComponentName(id.getResourceName());
						component.addField(langSpec.newFieldDeclaration(new Type(refResName, refResName), id.getResourceName()));
						constructor.addParameter(langSpec.newVariableDeclaration(new Type(refResName, refResName), id.getResourceName()));						
						constructor.getBody().addStatement(langSpec.getFieldAccessor(id.getResourceName()) + langSpec.getAssignment() + id.getResourceName() + langSpec.getStatementDelimiter());
					}
				}
			}
		}
	}
	
	protected MethodDeclaration getUpdateMethod(Edge inEdge, TypeDeclaration component,
			Map<Edge, Map<PushPullValue, List<ResourceNode>>> dataFlowInform, ILanguageSpecific langSpec) {
		List<ResourceNode> passedResoueces = dataFlowInform.get(inEdge).get(PushPullValue.PUSH);
		String methodName = updateMethodName;
		for (ResourceNode rn: passedResoueces) {
			ResourcePath rId = rn.getResource();
			methodName += langSpec.toComponentName(rId.getResourceName());
		}
		return getMethod(component, methodName);
	}

	protected MethodDeclaration getInputMethod(ResourceNode resourceNode, DataTransferChannel ch, TypeDeclaration component) {
		MethodDeclaration input = null;
		for (ChannelMember out : ch.getOutputChannelMembers()) {
			if (out.getResource().equals(resourceNode.getResource())) {
				Expression message = out.getStateTransition().getMessageExpression();
				if (message instanceof Term) {
					input = getMethod(component, ((Term) message).getSymbol().getImplName());
				} else if (message instanceof Variable) {
					// Declare an input method in this component.
					input = getMethod(component, ((Variable) message).getName());
				}
				break;
			}
		}
		return input;
	}

	protected MethodDeclaration getMethod(TypeDeclaration component, String methodName) {
		for (MethodDeclaration m: component.getMethods()) {
			if (m.getName().equals(methodName)) return m;
		}
		return null;
	}

	protected IResourceStateAccessor getPushAccessor() {
		return new IResourceStateAccessor() {
			@Override
			public Expression getCurrentStateAccessorFor(ResourcePath target, ResourcePath from) {
				if (target.equals(from)) {
					return new Field(fieldOfResourceState,
							target.getResourceStateType() != null ? target.getResourceStateType()
									: DataConstraintModel.typeInt);
				}
				// for reference channel member
				return new Parameter(target.getResourceName(),
						target.getResourceStateType() != null ? target.getResourceStateType()
								: DataConstraintModel.typeInt);
			}

			@Override
			public Expression getNextStateAccessorFor(ResourcePath target, ResourcePath from) {
				return new Parameter(target.getResourceName(),
						target.getResourceStateType() != null ? target.getResourceStateType()
								: DataConstraintModel.typeInt);
			}
		};
	}

	protected IResourceStateAccessor getPullAccessor() {
		return new IResourceStateAccessor() {
			@Override
			public Expression getCurrentStateAccessorFor(ResourcePath target, ResourcePath from) {
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
			public Expression getNextStateAccessorFor(ResourcePath target, ResourcePath from) {
				Term getter = new Term(new Symbol(getterOfResourceState, 1, Symbol.Type.METHOD));
				getter.addChild(new Field(target.getResourceName(), target.getResourceStateType()));
				return getter;
			}
		};
	}
}
