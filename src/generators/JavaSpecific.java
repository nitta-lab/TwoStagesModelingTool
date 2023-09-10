package generators;

import java.util.ArrayList;
import java.util.List;

import code.ast.CompilationUnit;
import code.ast.FieldDeclaration;
import code.ast.ImportDeclaration;
import code.ast.MethodDeclaration;
import code.ast.TypeDeclaration;
import code.ast.VariableDeclaration;
import models.algebra.Expression;
import models.algebra.Parameter;
import models.algebra.Term;
import models.algebra.Type;
import models.algebra.Variable;
import models.dataConstraintModel.DataConstraintModel;

public class JavaSpecific implements ILanguageSpecific {
	public static final Type typeVoid = new Type("Void", "void");
	public static final String self = "this";

	@Override
	public CompilationUnit newCompilationUnit(TypeDeclaration component) {
		CompilationUnit cu = new CompilationUnit(component);
		cu.addImport(new ImportDeclaration("java.util.*"));
		return cu;
	}

	@Override
	public TypeDeclaration newTypeDeclaration(String typeName) {
		return new TypeDeclaration(typeName);
	}
	
	@Override
	public VariableDeclaration newVariableDeclaration(Type type, String varName) {
		return new VariableDeclaration(type, varName);
	}

	@Override
	public MethodDeclaration newMethodDeclaration(String methodName, Type returnType) {
		if (returnType == null) {
			returnType = typeVoid;
		}
		return new MethodDeclaration(methodName, returnType);
	}

	@Override
	public MethodDeclaration newMethodDeclaration(String methodName, boolean isConstructor, Type returnType, List<VariableDeclaration> parameters) {
		if (returnType == null) {
			returnType = typeVoid;
		}
		return new MethodDeclaration(methodName, isConstructor, returnType, parameters);
	}

	@Override
	public FieldDeclaration newFieldDeclaration(Type fieldType, String fieldName) {
		return new FieldDeclaration(fieldType, fieldName);
	}

	@Override
	public FieldDeclaration newFieldDeclaration(Type fieldType, String fieldName, String fieldInitializer) {
		return new FieldDeclaration(fieldType, fieldName, fieldInitializer);
	}
	
	@Override
	public Type newListType(String compTypeName) {
		return new Type("List", "ArrayList<>", "List<" + compTypeName + ">", DataConstraintModel.typeList);		
	}
	
	@Override
	public Type newMapType(Type keyType, String valueTypeName) {
		return new Type("Map", "HashMap<>", "Map<" + keyType.getImplementationTypeName() + ", " + valueTypeName + ">", DataConstraintModel.typeMap);		
	}
	
	@Override
	public Type newTupleType(List<Type> componentTypes) {
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
		Type newTupleType = new Type("Tuple", implTypeName, interfaceTypeName, DataConstraintModel.typeTuple);
		return newTupleType;
	}

	@Override
	public String getVariableDeclaration(String typeName, String varName) {
		return typeName + " " + varName;
	}

	@Override
	public String getFieldInitializer(Type type, Expression initialValue) {
		String initializer = null;
		if (initialValue != null) {
			initializer = initialValue.toImplementation(new String[] {""});
		} else {
			if (DataConstraintModel.typeList.isAncestorOf(type)) {
				initializer = "new " + type.getImplementationTypeName() + "()";
			} else if (DataConstraintModel.typeMap.isAncestorOf(type)) {
				initializer = "new " + type.getImplementationTypeName() + "()";
			}
		}
		return initializer;
	}

	@Override
	public boolean declareField() {
		return true;
	}
	
	@Override
	public String getFieldAccessor(String fieldName) {
		return self + "." + fieldName;
	}
	
	@Override
	public String getMethodInvocation(String methodName) {
		return self + "." + methodName + "()";
	}

	@Override
	public String getMethodInvocation(String receiverName, String methodName) {
		return receiverName + "." + methodName + "()";
	}

	@Override
	public String getMethodInvocation(String receiverName, String methodName, List<String> parameters) {
		if (parameters == null) return getMethodInvocation(receiverName, methodName);
		String invocation = receiverName + "." + methodName + "(";
		if (parameters.size() > 0) {
			for (int i = 0; i < parameters.size(); i++) {
				if (i < parameters.size() - 1) {
					invocation += parameters.get(i) + ", ";
				} else {
					invocation += parameters.get(i);
				}
			}
		}
		invocation += ")";
		return invocation;
	}

	@Override
	public String getConstructorInvocation(String componentName, List<String> parameters) {
		String invocation = "new " + componentName + "(";
		if (parameters.size() > 0) {
			for (int i = 0; i < parameters.size(); i++) {
				if (i < parameters.size() - 1) {
					invocation += parameters.get(i) + ", ";
				} else {
					invocation += parameters.get(i);
				}
			}
		}
		invocation += ")";
		return invocation;
	}

	@Override
	public String getReturnStatement(String returnValue) {
		return "return " + returnValue;
	}

	@Override
	public String toComponentName(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	@Override
	public String toVariableName(String name) {
		return name.substring(0, 1).toLowerCase() + name.substring(1);
	}

	@Override
	public String getMainComponentName() {
		return "Main";
	}

	@Override
	public String getAssignment() {
		return " = ";
	}

	@Override
	public String getStatementDelimiter() {
		return ";";
	}

	@Override
	public String getTupleGet(String tupleExp, int idx, int length) {
		Expression t = new Variable(tupleExp, DataConstraintModel.typeTuple);
		for (int i = 0; i < idx; i++) {
			Term next = new Term(DataConstraintModel.snd);			
			next.addChild(t);
			t = next;
		}
		if (idx < length - 1) {
			Term last = new Term(DataConstraintModel.fst);
			last.addChild(t);
			t = last;
		}
		return t.toImplementation(new String[]{});
	}

	@Override
	public String getDecomposedTuple(String tupleExp, VariableDeclaration tupleVar, List<VariableDeclaration> vars) {
		String statements = "";
		statements += getVariableDeclaration(tupleVar.getType().getInterfaceTypeName(), tupleVar.getName())
							+ getAssignment() + tupleExp + getStatementDelimiter();
		for (int i = 0; i < vars.size(); i++) {
			VariableDeclaration var = vars.get(i);
			statements += "\n" + getVariableDeclaration(var.getType().getInterfaceTypeName(), var.getName())
							+ getAssignment()
							+ getTupleGet(tupleVar.getName(), i, vars.size())
							+ getStatementDelimiter();
		}
		return statements;
	}

	@Override
	public boolean isValueType(Type type) {
		if (type == DataConstraintModel.typeInt 
				|| type == DataConstraintModel.typeLong
				|| type == DataConstraintModel.typeFloat
				|| type == DataConstraintModel.typeDouble
				|| type == DataConstraintModel.typeBoolean) {
			return true;
		}
		return false;
	}

	
	@Override
	public boolean isVoidType(Type type) {
		if (type == typeVoid) {
			return true;
		}
		return false;
	}
	
	private String getImplementationTypeName(Type type) {
		if (type == null)
			return "Object";
		String wrapperType = DataConstraintModel.getWrapperType(type);
		if (wrapperType != null)
			return wrapperType;
		return type.getImplementationTypeName();
	}

	private String getInterfaceTypeName(Type type) {
		if (type == null)
			return "Object";
		String wrapperType = DataConstraintModel.getWrapperType(type);
		if (wrapperType != null)
			return wrapperType;
		return type.getInterfaceTypeName();
	}
}
