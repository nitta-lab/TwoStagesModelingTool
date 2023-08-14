package generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import code.ast.Block;
import code.ast.CompilationUnit;
import code.ast.FieldDeclaration;
import code.ast.ImportDeclaration;
import code.ast.MethodDeclaration;
import code.ast.TypeDeclaration;
import code.ast.VariableDeclaration;
import models.Edge;
import models.Node;
import models.algebra.Expression;
import models.algebra.Field;
import models.algebra.Parameter;
import models.algebra.Symbol;
import models.algebra.Term;
import models.algebra.Type;
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
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataFlowGraph;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.StoreAttribute;

/**
 * Generator for plain Java prototypes
 * 
 * @author Nitta
 *
 */
public class JavaCodeGenerator {
	public static final Type typeVoid = new Type("Void", "void");
	private static String defaultMainTypeName = "Main";
	static String mainTypeName = defaultMainTypeName;

	public static String getMainTypeName() {
		return mainTypeName;
	}

	public static void setMainTypeName(String mainTypeName) {
		JavaCodeGenerator.mainTypeName = mainTypeName;
	}

	public static void resetMainTypeName() {
		JavaCodeGenerator.mainTypeName = defaultMainTypeName;
	}

	static public ArrayList<CompilationUnit> doGenerate(DataFlowGraph graph, DataTransferModel model) {
		ArrayList<CompilationUnit> codes = new ArrayList<>();
		ArrayList<ResourceNode> resources = determineResourceOrder(graph);

		TypeDeclaration mainType = new TypeDeclaration(mainTypeName);
		CompilationUnit mainCU = new CompilationUnit(mainType);
		mainCU.addImport(new ImportDeclaration("java.util.*"));
		codes.add(mainCU);
		
		// Declare the constructor of the main type. 
		MethodDeclaration mainConstructor = new MethodDeclaration(mainTypeName, true);
		mainType.addMethod(mainConstructor);
		
		// For each resource.
		for (ResourceNode rn: resources) {
			boolean f = false;
			String resourceName = rn.getResource().getResourceName().substring(0, 1).toUpperCase()
					+ rn.getResource().getResourceName().substring(1);
			TypeDeclaration type = new TypeDeclaration(resourceName);
			
			// Declare the field to refer to each resource in the main type.
			String fieldInitializer = "new " + resourceName + "(";
			Set<ResourcePath> depends = new HashSet<>();
			for (Edge e : rn.getOutEdges()) {
				DataFlowEdge re = (DataFlowEdge) e;
				ResourcePath dstRes = ((ResourceNode) re.getDestination()).getResource();
				String resName = dstRes.getResourceName().substring(0, 1).toUpperCase() + dstRes.getResourceName().substring(1);
				if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
					depends.add(dstRes);
					fieldInitializer += resName.toLowerCase() + ",";
					f = true;
				}
			}
			for (Edge e : rn.getInEdges()) {
				DataFlowEdge re = (DataFlowEdge) e;
				ResourcePath srcRes = ((ResourceNode) re.getSource()).getResource();
				String resName = srcRes.getResourceName().substring(0, 1).toUpperCase() + srcRes.getResourceName().substring(1);
				if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) != PushPullValue.PUSH) {
					depends.add(srcRes);
					fieldInitializer += resName.toLowerCase() + ",";
					f = true;
				} else {
					if (rn.getIndegree() > 1) {
						// Declare a field to cash the state of the source resource in the type of the destination resource.
						ResourcePath cashResId = ((ResourceNode) re.getSource()).getResource();
						type.addField(new FieldDeclaration(
								cashResId.getResourceStateType(), ((ResourceNode) re.getSource()).getResource().getResourceName(), getInitializer(cashResId)));
					}
				}
			}
			Set<ResourcePath> refs = new HashSet<>();
			for (Channel cg : model.getChannels()) {
				DataTransferChannel c = (DataTransferChannel) cg;
				if (c.getInputResources().contains(rn.getResource())) {
					for (ResourcePath id: c.getReferenceResources()) {
						if (!refs.contains(id) && !depends.contains(id)) {
							refs.add(id);
							String refResName = id.getResourceName();
							fieldInitializer += refResName.toLowerCase() + ",";
							f = true;
						}
					}
				}
			}
			if (f) fieldInitializer = fieldInitializer.substring(0, fieldInitializer.length() - 1);
			fieldInitializer += ")";
			FieldDeclaration field = new FieldDeclaration(new Type(resourceName, resourceName), rn.getResource().getResourceName());
			mainType.addField(field);
			Block manConstructorBody = mainConstructor.getBody();
			if (manConstructorBody == null) {
				manConstructorBody = new Block();
				mainConstructor.setBody(manConstructorBody);
			}
			manConstructorBody.addStatement(rn.getResource().getResourceName() + " = " + fieldInitializer + ";");
			
			// Declare a constructor, fields and update methods in the type of each resource.
			MethodDeclaration constructor = new MethodDeclaration(resourceName, true);
			Block block = new Block();
			depends = new HashSet<>();
			for (Edge e : rn.getOutEdges()) {
				DataFlowEdge re = (DataFlowEdge) e;
				ResourcePath dstRes = ((ResourceNode) re.getDestination()).getResource();
				String dstResName = dstRes.getResourceName().substring(0, 1).toUpperCase() + dstRes.getResourceName().substring(1);
				if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
					// Declare a field to refer to the destination resource of push transfer.
					depends.add(dstRes);
					type.addField(new FieldDeclaration(new Type(dstResName, dstResName), dstRes.getResourceName()));
					constructor.addParameter(new VariableDeclaration(new Type(dstResName, dstResName), dstRes.getResourceName()));
					block.addStatement("this." + dstResName.toLowerCase() + " = " + dstResName.toLowerCase() + ";");
				}
			}
			for (Edge e : rn.getInEdges()) {
				DataFlowEdge re = (DataFlowEdge) e;
				ResourcePath srcRes = ((ResourceNode) re.getSource()).getResource();
				String srcResName = srcRes.getResourceName().substring(0, 1).toUpperCase() + srcRes.getResourceName().substring(1);
				if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) != PushPullValue.PUSH) {
					// Declare a field to refer to the source resource of pull transfer.
					depends.add(srcRes);
					type.addField(new FieldDeclaration(new Type(srcResName, srcResName), srcRes.getResourceName()));
					constructor.addParameter(new VariableDeclaration(new Type(srcResName, srcResName), srcRes.getResourceName()));
					block.addStatement("this." + srcResName.toLowerCase() + " = " + srcResName.toLowerCase() + ";");
				} else {
					// Declare an update method in the type of the destination resource.
					ArrayList<VariableDeclaration> vars = new ArrayList<>();
					vars.add(new VariableDeclaration(srcRes.getResourceStateType(), srcRes.getResourceName()));
					DataTransferChannel c = (DataTransferChannel) re.getChannel();
					for (ResourcePath ref: c.getReferenceResources()) {
						if (!ref.equals(rn.getResource())) {
							vars.add(new VariableDeclaration(ref.getResourceStateType(), ref.getResourceName()));
						}
					}
					type.addMethod(new MethodDeclaration("update" + srcResName, false, typeVoid, vars));
				}
			}
			// Declare a field to refer to the reference resource.
			refs = new HashSet<>();
			for (Channel cg : model.getChannels()) {
				DataTransferChannel c = (DataTransferChannel) cg;
				if (c.getInputResources().contains(rn.getResource())) {
					for (ResourcePath id: c.getReferenceResources()) {
						if (!refs.contains(id) && !depends.contains(id)) {
							refs.add(id);
							String refResName = id.getResourceName();
							refResName = refResName.substring(0, 1).toUpperCase() + refResName.substring(1);
							type.addField(new FieldDeclaration(new Type(refResName, refResName), id.getResourceName()));
							constructor.addParameter(new VariableDeclaration(new Type(refResName, refResName), id.getResourceName()));						
							block.addStatement("this." + id.getResourceName() + " = " + id.getResourceName() + ";");
						}
					}
				}
			}
			constructor.setBody(block);
			if (constructor.getParameters() != null)
				type.addMethod(constructor);
			
			// Declare input methods in resources and the main type.
			for (Channel cg : model.getIOChannels()) {
				for (ChannelMember cm : ((DataTransferChannel) cg).getOutputChannelMembers()) {
					if (cm.getResource().equals(rn.getResource())) {
						Expression message = cm.getStateTransition().getMessageExpression();
						if (message.getClass() == Term.class) {
							ArrayList<VariableDeclaration> params = new ArrayList<>();
							for (Variable var: message.getVariables().values()) {
								params.add(new VariableDeclaration(var.getType(), var.getName()));
							}
							MethodDeclaration input = new MethodDeclaration(
									((Term) cm.getStateTransition().getMessageExpression()).getSymbol().getImplName(),
									false, typeVoid, params);
							type.addMethod(input);
							String str = ((Term) cm.getStateTransition().getMessageExpression()).getSymbol().getImplName();
							input = getMethod(mainType, str);
							if (input == null) {
								input = new MethodDeclaration(str, false, typeVoid, params);
								mainType.addMethod(input);
							} else {
								// Add type to a parameter without type.
								for (VariableDeclaration param: input.getParameters()) {
									if (param.getType() == null) {
										for (VariableDeclaration p: params) {
											if (param.getName().equals(p.getName()) && p.getType() != null) {
												param.setType(p.getType());
											}
										}
									}
								}
							}
						} else if (message.getClass() == Variable.class) {
							MethodDeclaration input = new MethodDeclaration(
									((Variable) cm.getStateTransition().getMessageExpression()).getName(),
									false, typeVoid, null);
							type.addMethod(input);
							String str = ((Variable) cm.getStateTransition().getMessageExpression()).getName();
							input = getMethod(mainType, str);
							if (input == null) {
								input = new MethodDeclaration(str, false, typeVoid, null);
								mainType.addMethod(input);
							}
						}
					}
				}
			}
			
			// Declare the field to store the state in the type of each resource.
			if (((StoreAttribute) rn.getAttribute()).isStored()) {
				ResourcePath resId = rn.getResource();
				type.addField(new FieldDeclaration(resId.getResourceStateType(), "value", getInitializer(resId)));
			}
			
			// Declare the getter method to obtain the state in the type of each resource.
			type.addMethod(new MethodDeclaration("getValue",
					rn.getResource().getResourceStateType()));
			
			// Add compilation unit for each resource.
			CompilationUnit cu = new CompilationUnit(type);
			cu.addImport(new ImportDeclaration("java.util.*"));
			codes.add(cu);
		}

		// Declare the Pair class.
		boolean isCreatedPair = false;
		for(ResourceNode rn : resources) {
			if(isCreatedPair) continue;
			if(model.getType("Pair").isAncestorOf(rn.getResource().getResourceStateType())) {
				TypeDeclaration type = new TypeDeclaration("Pair<T>");
				type.addField(new FieldDeclaration(new Type("Double", "T"), "left"));
				type.addField(new FieldDeclaration(new Type("Double", "T"), "right"));
				
				MethodDeclaration constructor = new MethodDeclaration("Pair", true);
				constructor.addParameter(new VariableDeclaration(new Type("Double", "T"), "left"));
				constructor.addParameter(new VariableDeclaration(new Type("Double", "T"), "right"));
				Block block = new Block();
				block.addStatement("this.left = left;");
				block.addStatement("this.right = right;");
				constructor.setBody(block);
				type.addMethod(constructor);
			
				for(FieldDeclaration field : type.getFields()) {
					MethodDeclaration getter = new MethodDeclaration(
						"get" + field.getName().substring(0,1).toUpperCase() + field.getName().substring(1),
						new Type("Double","T"));
					getter.setBody(new Block());
					getter.getBody().addStatement("return " + field.getName() + ";");
					type.addMethod(getter);
				}
			
				CompilationUnit	cu = new CompilationUnit(type);
				cu.addImport(new ImportDeclaration("java.util.*"));
				codes.add(cu);
				
				isCreatedPair = true;
			}
		}
		
		// Declare getter methods in the main type.
		for (Node n : graph.getNodes()) {
			ResourceNode rn = (ResourceNode) n;
			MethodDeclaration getter = new MethodDeclaration(
					"get" + rn.getResource().getResourceName().substring(0, 1).toUpperCase()
							+ rn.getResource().getResourceName().substring(1),
					rn.getResource().getResourceStateType());
			getter.setBody(new Block());
			getter.getBody().addStatement(
					"return " + rn.getResource().getResourceName() + ".getValue();");
			mainType.addMethod(getter);
		}
		
		
		HashSet<String> tmps = new HashSet<>();
		HashSet<String> cont = new HashSet<>();
		for (MethodDeclaration method : mainType.getMethods()) {
			if (!tmps.contains(method.getName()))
				tmps.add(method.getName());
			else
				cont.add(method.getName());
		}
		for (MethodDeclaration method : mainType.getMethods()) {
			if (cont.contains(method.getName())) {
				method.setName(method.getName() + method.getParameters().get(0).getName().substring(0, 1).toUpperCase()
						+ method.getParameters().get(0).getName().substring(1));
			}
		}
		return codes;
	}

	private static String getInitializer(ResourcePath resId) {
		Type stateType = resId.getResourceStateType();
		String initializer = null;
		if (resId.getInitialValue() != null) {
			initializer = resId.getInitialValue().toImplementation(new String[] {""});
		} else {
			if (DataConstraintModel.typeList.isAncestorOf(stateType)) {
				initializer = "new " + resId.getResourceStateType().getImplementationTypeName() + "()";
			} else if (DataConstraintModel.typeMap.isAncestorOf(stateType)) {
				initializer = "new " + resId.getResourceStateType().getImplementationTypeName() + "()";
			}
		}
		return initializer;
	}

	static public ArrayList<String> getCodes(ArrayList<TypeDeclaration> codeTree) {
		ArrayList<String> codes = new ArrayList<>();
		for (TypeDeclaration type : codeTree) {
			codes.add("public class " + type.getTypeName() + "{");
			for (FieldDeclaration field : type.getFields()) {
				if (type.getTypeName() != mainTypeName) {
					String cons = "\t" + "private " + field.getType().getInterfaceTypeName() + " "
							+ field.getName();
					if (DataConstraintModel.isListType(field.getType()))
						cons += " = new ArrayList<>()";
					cons += ";";
					codes.add(cons);
				} else {
					String cons = "\t" + "private " + field.getType().getInterfaceTypeName() + " "
							+ field.getName() + " = new " + field.getType().getTypeName() + "(";
					cons += ");";
					codes.add(cons);
				}
			}
			codes.add("");
			for (MethodDeclaration method : type.getMethods()) {
				String varstr = "\t" + "public " + method.getReturnType().getInterfaceTypeName() + " "
						+ method.getName() + "(";
				if (method.getParameters() != null) {
					for (VariableDeclaration var : method.getParameters()) {
						varstr += var.getType().getInterfaceTypeName() + " " + var.getName() + ",";
					}
					if (!method.getParameters().isEmpty())
						varstr = varstr.substring(0, varstr.length() - 1);
				}
				if (method.getBody() != null) {
					for (String str : method.getBody().getStatements()) {
						codes.add("\t\t" + str + ";");
					}
				}
				codes.add(varstr + ")" + "{");
				codes.add("\t" + "}");
				codes.add("");
			}
			codes.add("}");
			codes.add("");
		}
		return codes;
	}

	static private ArrayList<ResourceNode> determineResourceOrder(DataFlowGraph graph) {
		ArrayList<ResourceNode> resources = new ArrayList<>();
		Set<ResourceNode> visited = new HashSet<>();
		for (Node n : graph.getNodes()) {
			ResourceNode rn = (ResourceNode) n;
			topologicalSort(graph, rn, visited, resources);
		}
		return resources;
	}
	
	static private void topologicalSort(DataFlowGraph graph, ResourceNode curNode, Set<ResourceNode> visited, List<ResourceNode> orderedList) {
		if (visited.contains(curNode)) return;
		visited.add(curNode);
		for (Edge e : curNode.getInEdges()) {
			DataFlowEdge re = (DataFlowEdge) e;
			if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
				topologicalSort(graph, (ResourceNode) re.getSource(), visited, orderedList);
			}
		}
		for (Edge e : curNode.getOutEdges()) {
			DataFlowEdge re = (DataFlowEdge) e;
			if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) != PushPullValue.PUSH) {
				topologicalSort(graph, (ResourceNode) re.getDestination(), visited, orderedList);
			}
		}
		for (Node n: graph.getNodes()) {		// for reference resources.
			ResourceNode rn = (ResourceNode) n;
			for (Edge e : rn.getOutEdges()) {
				DataFlowEdge re = (DataFlowEdge) e;
				for (ChannelMember m: re.getChannel().getReferenceChannelMembers()) {
					if (m.getResource().equals(curNode.getResource())) {
						topologicalSort(graph, rn, visited, orderedList);
					}
				}
			}
		}
		orderedList.add(0, curNode);
	}

	private static MethodDeclaration getMethod(TypeDeclaration type, String methodName) {
		for (MethodDeclaration m: type.getMethods()) {
			if (m.getName().equals(methodName)) return m;
		}
		return null;
	}

	static public IResourceStateAccessor pushAccessor = new IResourceStateAccessor() {
		@Override
		public Expression getCurrentStateAccessorFor(ResourcePath target, ResourcePath from) {
			if (target.equals(from)) {
				return new Field("value",
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
	static public IResourceStateAccessor pullAccessor = new IResourceStateAccessor() {
		@Override
		public Expression getCurrentStateAccessorFor(ResourcePath target, ResourcePath from) {
			if (target.equals(from)) {
				return new Field("value",
						target.getResourceStateType() != null ? target.getResourceStateType()
								: DataConstraintModel.typeInt);
			}
			// for reference channel member
			Term getter = new Term(new Symbol("getValue", 1, Symbol.Type.METHOD));
			getter.addChild(new Field(target.getResourceName(), target.getResourceStateType()));
			return getter;
		}

		@Override
		public Expression getNextStateAccessorFor(ResourcePath target, ResourcePath from) {
			Term getter = new Term(new Symbol("getValue", 1, Symbol.Type.METHOD));
			getter.addChild(new Field(target.getResourceName(), target.getResourceStateType()));
			return getter;
		}
	};
}
