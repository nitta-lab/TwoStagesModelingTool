package code.ast;

import java.util.ArrayList;
import java.util.List;

public class CompilationUnit extends ASTNode {
	private String fileName = null;
	private List<ImportDeclaration> imports = new ArrayList<>();
	private List<TypeDeclaration> types = new ArrayList<>();
	
	public CompilationUnit(TypeDeclaration type) {
		types.add(type);
		if(type.getTypeName().contains("<")) 
			fileName = type.getTypeName().split("<")[0] + ".java";
		else
			fileName = type.getTypeName() + ".java";
	}
	
	public List<ImportDeclaration> imports() {
		return imports;
	}
	
	public List<TypeDeclaration> types() {
		return types;
	}
	
	public void addImport(ImportDeclaration imp) {
		imports.add(imp);
	}
	
	public void addType(TypeDeclaration type) {
		types.add(type);
	}

	public String getFileName() {
		return fileName;
	}
	
	public String toString() {
		String result = "";
		for (ImportDeclaration imp: imports) {
			result += imp.toString();
		}
		result +="\n";
		for (TypeDeclaration type: types) {
			result += type.toString();
		}
		return result;
	}
}
