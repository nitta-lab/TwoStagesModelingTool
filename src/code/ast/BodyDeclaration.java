package code.ast;

public abstract class BodyDeclaration extends ASTNode {
	private int modifiers = 0;

	public int getModifiers() {
		return modifiers;
	}

	public void setModifiers(int modifiers) {
		this.modifiers = modifiers;
	}
}
