package code.ast;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class TypeDeclaration extends AbstractTypeDeclaration implements IAnnotatable {
	private List<FieldDeclaration> fields = new ArrayList<>();
	private List<MethodDeclaration> methods = new ArrayList<>();
	private Map<String, Annotation> annotations = new HashMap<>();
	
	public TypeDeclaration(String typeName) {
		this.typeName = typeName;
	}
	
	public TypeDeclaration(String typeName, List<FieldDeclaration> fields) {
		this.typeName = typeName;
		this.fields = fields;
	}
	
	public TypeDeclaration(String typeName, List<FieldDeclaration> fields, List<MethodDeclaration> methods) {
		this.typeName = typeName;
		this.fields = fields;
		this.methods = methods;
	}
	
	public void addField(FieldDeclaration field) {
		fields.add(field);
	}
	
	public void addMethod(MethodDeclaration method) {
		methods.add(method);
	}

	public void removeMethod(MethodDeclaration method) {
		methods.remove(method);
	}

	public List<FieldDeclaration> getFields() {
		return fields;
	}
	
	public List<MethodDeclaration> getMethods() {
		return methods;
	}

	public MethodDeclaration createConstructor() {
		MethodDeclaration constructor = new MethodDeclaration(typeName, true);
		addMethod(constructor);
		return constructor;
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
		code += "public class " + typeName + " {\n";
		for (FieldDeclaration f: fields) {
			code += "\t" + f.toString();
		}
		for (MethodDeclaration m: methods) {
			code += CodeUtil.insertTab(m.toString());
		}
		code += "}";
		return code;
	}
}
