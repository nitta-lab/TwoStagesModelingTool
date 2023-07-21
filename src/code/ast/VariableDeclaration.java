package code.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import models.algebra.Type;

public class VariableDeclaration extends ASTNode implements IAnnotatable {
	private Type type;
	private String variableName;
	private Map<String, Annotation> annotations = new HashMap<>();
	
	public VariableDeclaration(Type type, String variableName) {
		this.type = type;
		this.variableName = variableName;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getName() {
		return variableName;
	}

	public void setName(String variableName) {
		this.variableName = variableName;
	}

	@Override
	public Annotation getAnnotation(String name) {
		return annotations.get(name);
	}

	@Override
	public Collection<Annotation> getAnnotations() {
		return annotations.values();
	}

	@Override
	public void addAnnotation(Annotation annotation) {
		annotations.put(annotation.getElementName(), annotation);
	}
	
	public String toString() {
		String code = "";
		for (Annotation annotation: getAnnotations()) {
			code += annotation.toString() + " ";
		}
		code += type.getInterfaceTypeName() + " " + variableName;
		return code;
	}
}
