package code.ast;

import java.util.Set;
import java.util.HashSet;

public class Throws extends ASTNode {
	private Set<String> exceptions = new HashSet<>();

	public Throws() {
	}
	
	public void addException(String exception) {
		exceptions.add(exception);
	}

	public Set<String> getExceptions() {
		return exceptions;
	}
	
	public String toString() {
		String code = "throws ";
		String delimiter = "";
		for (String exception: exceptions) {
			code += delimiter + exception;
			delimiter = ", ";
		}
		return code;
	}
}
