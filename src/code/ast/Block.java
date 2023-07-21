package code.ast;

import java.util.List;
import java.util.ArrayList;

public class Block extends ASTNode {
	private List<String> statements = new ArrayList<String>();

	public List<String> getStatements() {
		return statements;
	}

	public void setStatements(List<String> statements) {
		this.statements = statements;
	}
	
	public void addFirstStatement(String statement) {
		statements.add(0, statement);
	}
	
	public void addStatement(String statement) {
		statements.add(statement);
	}
	
	public String toString() {
		String code = "";
		for (String statement: statements) {
			code += (statement + "\n");
		}
		return code;
	}
}
