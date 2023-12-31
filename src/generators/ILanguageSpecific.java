package generators;

import java.util.ArrayList;
import java.util.List;

import code.ast.CompilationUnit;
import code.ast.FieldDeclaration;
import code.ast.MethodDeclaration;
import code.ast.TypeDeclaration;
import code.ast.VariableDeclaration;
import models.algebra.Expression;
import models.algebra.Type;
import models.dataFlowModel.DataTransferChannel.IResourceStateAccessor;

public interface ILanguageSpecific {
	CompilationUnit newCompilationUnit(TypeDeclaration component);
	TypeDeclaration newTypeDeclaration(String typeName);
	VariableDeclaration newVariableDeclaration(Type type, String varName);
	MethodDeclaration newMethodDeclaration(String methodName, Type returnType);
	MethodDeclaration newMethodDeclaration(String methodName, boolean isConstructor, Type returnType, List<VariableDeclaration> parameters);
	FieldDeclaration newFieldDeclaration(Type fieldType, String fieldName);
	FieldDeclaration newFieldDeclaration(Type fieldType, String fieldName, String fieldInitializer);
	Type newListType(String compTypeName);
	Type newMapType(Type keyType, String compTypeName);
	Type newTupleType(List<Type> compTypes);
	String getVariableDeclaration(String typeName, String varName);
	String getFieldInitializer(Type type, Expression initialValue);
	boolean declareField();
	String getFieldAccessor(String fieldName);
	String getMethodInvocation(String methodName);
	String getMethodInvocation(String receivertName, String methodName);
	String getMethodInvocation(String receivertName, String methodName, List<String> parameters);
	String getConstructorInvocation(String componentName, List<String> parameters);
	String getReturnStatement(String returnValue);
	String toComponentName(String name);
	String toVariableName(String name);
	String getMainComponentName();
	String getTupleGet(String tupleExp, int idx, int length);
	String getDecomposedTuple(String tupleExp, VariableDeclaration tupleVar, List<VariableDeclaration> vars);
	String getAssignment();
	String getStatementDelimiter();
	String getStringDelimiter();
	boolean isValueType(Type type);
	boolean isVoidType(Type type);
}
