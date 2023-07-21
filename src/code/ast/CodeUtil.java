package code.ast;

public class CodeUtil {
	
	public static String insertTab(String code) {
		String newString = "";
		String[] lines = code.split("\n");
		for (String line: lines) {
			newString = newString + "\t" + line + "\n";
		}
		return newString;
	}

	public static String getToStringExp(String typeName, String rawExp) {
		if (typeName.equals("int")) {
			return "Integer.toString(" + rawExp + ")";
		} else if (typeName.equals("float")) {
			return "Float.toString(" + rawExp + ")";
		} else if (typeName.equals("double")) {
			return "Double.toString(" + rawExp + ")";
		} else if (typeName.equals("boolean")) {
			return "Boolean.toString(" + rawExp + ")";
		} else {			
			return rawExp + ".toString()";
		}
	}

	public static String getToValueExp(String typeName, String strExp) {
		if (typeName.equals("int")) {
			return "Integer.parseInt(" + strExp + ")";
		} else if (typeName.equals("float")) {
			return "Float.parseFloat(" + strExp + ")";
		} else if (typeName.equals("double")) {
			return "Double.parseDouble(" + strExp + ")";
		} else if (typeName.equals("boolean")) {
			return "Boolean.parseBoolean(" + strExp + ")";
		} else if (typeName.startsWith("ArrayList") || typeName.startsWith("List")) {
			return "Arrays.asList(" + strExp + ".replace(\"[\",\"\").replace(\"]\",\"\").split(\",\",0))";
		} else {
			return strExp;
		}
	}
}
