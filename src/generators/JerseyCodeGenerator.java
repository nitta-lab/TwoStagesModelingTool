package generators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import code.ast.Annotation;
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
import models.dataConstraintModel.ChannelGenerator;
import models.dataConstraintModel.ChannelMember;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.IdentifierTemplate;
import models.dataFlowModel.DataTransferModel;
import models.dataFlowModel.DataTransferChannelGenerator;
import models.dataFlowModel.DataTransferChannelGenerator.IResourceStateAccessor;
import models.dataFlowModel.PushPullAttribute;
import models.dataFlowModel.PushPullValue;
import models.dataFlowModel.DataFlowEdge;
import models.dataFlowModel.DataFlowGraph;
import models.dataFlowModel.ResourceNode;
import models.dataFlowModel.StoreAttribute;

/**
 * Generator for Jersey prototypes
 * 
 * @author Nitta
 *
 */
public class JerseyCodeGenerator {
	public static final Type typeVoid = new Type("Void", "void");
	public static final Type typeClient = new Type("Client", "Client");
	private static String defaultMainTypeName = "Main";
	static String mainTypeName = defaultMainTypeName;

	public static String getMainTypeName() {
		return mainTypeName;
	}

	public static void setMainTypeName(String mainTypeName) {
		JerseyCodeGenerator.mainTypeName = mainTypeName;
	}

	public static void resetMainTypeName() {
		JerseyCodeGenerator.mainTypeName = defaultMainTypeName;
	}

	static public ArrayList<CompilationUnit> doGenerate(DataFlowGraph graph, DataTransferModel model) {
		ArrayList<CompilationUnit> codes = new ArrayList<>();
//		ArrayList<ResourceNode> resources = StoreResourceCheck(graph);
		Set<Node> resources = graph.getNodes();

		for (Node n : resources) {
			ResourceNode rn = (ResourceNode) n;			
			String resourceName = rn.getIdentifierTemplate().getResourceName().substring(0, 1).toUpperCase()
					+ rn.getIdentifierTemplate().getResourceName().substring(1);

			// Declare the field to refer each resource in the main type.
			TypeDeclaration type = new TypeDeclaration(resourceName);
			type.addAnnotation(new Annotation("Component"));
			type.addAnnotation(new Annotation("Path", "\"/" + rn.getIdentifierTemplate().getResourceName() + "\""));
			
			// Declare a client field and update methods from other resources.
			boolean bDeclareClientField = false;
			for (Edge e : rn.getOutEdges()) {
				DataFlowEdge re = (DataFlowEdge) e;
				if (!bDeclareClientField && ((PushPullAttribute) re.getAttribute()).getOptions().get(0) == PushPullValue.PUSH) {
					// Declare a client field to connect to the destination resource of push transfer.
					type.addField(new FieldDeclaration(typeClient, "client", "ClientBuilder.newClient()"));
					bDeclareClientField = true;
				}
			}
			for (Edge e : rn.getInEdges()) {
				DataFlowEdge re = (DataFlowEdge) e;
				IdentifierTemplate srcRes = ((ResourceNode) re.getSource()).getIdentifierTemplate();
				String srcResName = srcRes.getResourceName().substring(0, 1).toUpperCase() + srcRes.getResourceName().substring(1);
				if (((PushPullAttribute) re.getAttribute()).getOptions().get(0) != PushPullValue.PUSH) {
					if (!bDeclareClientField) {
						// Declare a client field to connect to the source resource of pull transfer.
						type.addField(new FieldDeclaration(typeClient, "client", "ClientBuilder.newClient()"));
						bDeclareClientField = true;
					}
				} else {
					// Declare an update method in the type of the destination resource.
					ArrayList<VariableDeclaration> vars = new ArrayList<>();
					String srcName = srcRes.getResourceName();
					Type srcType = srcRes.getResourceStateType();
					VariableDeclaration param = new VariableDeclaration(srcType, srcName);
					param.addAnnotation(new Annotation("FormParam", "\"" + srcName + "\""));
					vars.add(param);
					for (IdentifierTemplate refRes: re.getChannelGenerator().getReferenceIdentifierTemplates()) {
						if (refRes != rn.getIdentifierTemplate()) {
							param = new VariableDeclaration(refRes.getResourceStateType(), refRes.getResourceName());
							param.addAnnotation(new Annotation("FormParam", "\"" + refRes.getResourceName() + "\""));
							vars.add(param);						
						}
					}
					MethodDeclaration update = new MethodDeclaration("update" + srcResName, false, typeVoid, vars);
					for (ChannelMember cm: re.getChannelGenerator().getOutputChannelMembers()) {
						if (cm.getIdentifierTemplate() == rn.getIdentifierTemplate()) {
							if (cm.getStateTransition().isRightUnary()) {
								update.addAnnotation(new Annotation("PUT"));
							} else {
								update.addAnnotation(new Annotation("POST"));
							}
						}
					}
					if (rn.getInEdges().size() > 1) {
						 // For each source resource, a child resource is defined in the destination resource so that its state can be updated separately.
						update.addAnnotation(new Annotation("Path", "\"/" + srcName + "\""));
						// Declare a field to cash the state of the source resource in the type of the destination resource.
						IdentifierTemplate cashResId = ((ResourceNode) re.getSource()).getIdentifierTemplate();
						type.addField(new FieldDeclaration(cashResId.getResourceStateType(), srcName, getInitializer(cashResId)));
					}
					type.addMethod(update);
				}
			}
			
//			// Declare a client field to connect to the source resource of reference transfer.
//			if (!bDeclareClientField) {
//				for (ChannelGenerator cg : model.getChannelGenerators()) {
//					DataflowChannelGenerator dcg = ((DataflowChannelGenerator) cg);
//					for (ChannelMember cm : dcg.getOutputChannelMembers()) {
//						if (cm.getIdentifierTemplate().getResourceName().equals(type.getTypeName().toLowerCase())) {
//							if (dcg.getReferenceChannelMembers().size() > 0) {
//								// If there exists one or more reference channel member.
//								type.addField(new FieldDeclaration(typeClient, "client", "ClientBuilder.newClient()"));
//								bDeclareClientField = true;
//								break;
//							}
//						}
//					}
//					if (bDeclareClientField) break;
//				}
//			}
			
			// Declare input methods in resources.
			for (ChannelGenerator cg : model.getIOChannelGenerators()) {
				for (ChannelMember cm : ((DataTransferChannelGenerator) cg).getOutputChannelMembers()) {
					if (cm.getIdentifierTemplate().equals(rn.getIdentifierTemplate())) {
						Expression message = cm.getStateTransition().getMessageExpression();
						if (message.getClass() == Term.class) {
							ArrayList<VariableDeclaration> params = new ArrayList<>();
							for (Variable var: message.getVariables().values()) {
								String paramName = var.getName();
								VariableDeclaration param = new VariableDeclaration(var.getType(), paramName);
								param.addAnnotation(new Annotation("FormParam", "\"" + paramName + "\""));
								params.add(param);
							}
							MethodDeclaration input = new MethodDeclaration(
									((Term) cm.getStateTransition().getMessageExpression()).getSymbol().getImplName(),
									false, typeVoid, params);
							if (cm.getStateTransition().isRightUnary()) {
								input.addAnnotation(new Annotation("PUT"));
							} else {
								input.addAnnotation(new Annotation("POST"));
							}
							type.addMethod(input);
						} else if (message.getClass() == Variable.class) {
							MethodDeclaration input = new MethodDeclaration(
									((Variable) cm.getStateTransition().getMessageExpression()).getName(),
									false, typeVoid, null);
							if (cm.getStateTransition().isRightUnary()) {
								input.addAnnotation(new Annotation("PUT"));
							} else {
								input.addAnnotation(new Annotation("POST"));
							}
							type.addMethod(input);
						}
					}
				}
			}
			
			// Declare the field to store the state in the type of each resource.
			if (((StoreAttribute) rn.getAttribute()).isStored()) {
				IdentifierTemplate resId = rn.getIdentifierTemplate();
				type.addField(new FieldDeclaration(resId.getResourceStateType(), "value", getInitializer(resId)));
			}
			
			// Declare the getter method to obtain the state in the type of each resource.
			MethodDeclaration getter = new MethodDeclaration("getValue", rn.getIdentifierTemplate().getResourceStateType());
			getter.addAnnotation(new Annotation("Produces", "MediaType.APPLICATION_JSON"));
			getter.addAnnotation(new Annotation("GET"));
			type.addMethod(getter);
			
			// Add compilation unit for each resource.
			CompilationUnit cu = new CompilationUnit(type);
			cu.addImport(new ImportDeclaration("java.util.*"));
			cu.addImport(new ImportDeclaration("javax.ws.rs.*"));
			cu.addImport(new ImportDeclaration("javax.ws.rs.client.*"));
			cu.addImport(new ImportDeclaration("javax.ws.rs.core.*"));
			cu.addImport(new ImportDeclaration("org.springframework.stereotype.Component"));
			cu.addImport(new ImportDeclaration("com.fasterxml.jackson.databind.ObjectMapper"));
			cu.addImport(new ImportDeclaration("com.fasterxml.jackson.core.JsonProcessingException"));
			codes.add(cu);
		}
		
		// Declare the Pair class.
		boolean isCreatedPair = false;
		for(Node n : resources) {
			ResourceNode rn = (ResourceNode) n;
			if(isCreatedPair) continue;
			if(model.getType("Pair").isAncestorOf(rn.getIdentifierTemplate().getResourceStateType())) {
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
				
//				MethodDeclaration toStr = new MethodDeclaration("toString", false, DataConstraintModel.typeString, null);
//				block = new Block();
//				block.addStatement("return \"{\\\"\" + left + \"\\\":\\\"\" + right + \"\\\"}\";");
//				toStr.setBody(block);
//				type.addMethod(toStr);
			
				CompilationUnit	cu = new CompilationUnit(type);
				cu.addImport(new ImportDeclaration("java.util.*"));
				codes.add(cu);
				
				isCreatedPair = true;
			}
		}
				
		return codes;
	}

	private static String getInitializer(IdentifierTemplate resId) {
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
						cons += " = new " + field.getType().getImplementationTypeName() + "()";
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

	static public IResourceStateAccessor pushAccessor = new IResourceStateAccessor() {
		@Override
		public Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
			if (target.equals(from)) {
				return new Field("value",
						target.getResourceStateType() != null ? target.getResourceStateType()
								: DataConstraintModel.typeInt);
			}
			return null;
		}

		@Override
		public Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
			return new Parameter(target.getResourceName(),
					target.getResourceStateType() != null ? target.getResourceStateType()
							: DataConstraintModel.typeInt);
		}
	};
	static public IResourceStateAccessor pullAccessor = new IResourceStateAccessor() {
		@Override
		public Expression getCurrentStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
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
		public Expression getNextStateAccessorFor(IdentifierTemplate target, IdentifierTemplate from) {
			return new Parameter(target.getResourceName(),
					target.getResourceStateType() != null ? target.getResourceStateType()
							: DataConstraintModel.typeInt);
		}
	};
}
