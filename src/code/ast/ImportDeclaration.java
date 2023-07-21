package code.ast;

public class ImportDeclaration {
	private String name;
	
	public ImportDeclaration(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		return "import " + name + ";\n";
	}
}
