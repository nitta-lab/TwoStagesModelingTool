package code.ast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import models.algebra.Type;

public class FieldDeclaration extends BodyDeclaration implements IAnnotatable {
	private Type type;
	private String fieldName;
	private String initializer;
	private Map<String, Annotation> annotations = new HashMap<>();
	
	public FieldDeclaration(Type type, String fieldName) {
		this.type = type;
		this.fieldName = fieldName;
	}
	
	public FieldDeclaration(Type type, String fieldName, String initializer) {
		this.type = type;
		this.fieldName = fieldName;
		this.initializer = initializer;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getName() {
		return fieldName;
	}

	public void setName(String fieldName) {
		this.fieldName = fieldName;
	}	

	public String getInitializer() {
		return initializer;
	}

	public void setInitializer(String initializer) {
		this.initializer = initializer;
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
			code += annotation.toString() + "\n";
		}
		if (initializer == null) {
			code += "private " + type.getInterfaceTypeName() + " " + fieldName + ";\n";
		} else {
			code += "private " + type.getInterfaceTypeName() + " " + fieldName + " = " + initializer + ";\n";
		}
		return code;
	}
}
