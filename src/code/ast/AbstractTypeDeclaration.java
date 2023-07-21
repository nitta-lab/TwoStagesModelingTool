package code.ast;

public class AbstractTypeDeclaration extends BodyDeclaration {
	protected String typeName = null;

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
}
