package code.ast;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Annotation extends ASTNode {
	private String name = null;
	private Map<String, Object> keyValueMap = null;
	
	public Annotation(String name) {
		this.name = name;
		keyValueMap = new HashMap<>();
	}
	
	public Annotation(String name, String value) {
		this.name = name;
		keyValueMap = new HashMap<>();
		keyValueMap.put("value", value);
	}

	public String getElementName() {
		return name;
	}

	public Map<String, Object> getParams() {
		return keyValueMap;
	}
	
	public Object getValue(String key) {
		return keyValueMap.get(key);
	}
	
	public void addParam(String key, Object value) {
		keyValueMap.put(key, value);
	}
	
	public String toString() {
		String code = "@" + name;
		Set<String> keySet = keyValueMap.keySet();
		if (keySet.size() == 0) {
			return code;
		} else if (keySet.size() == 1 && keySet.iterator().next().equals("value")) {
			code += "(" + keyValueMap.get("value").toString() + ")";
		} else {
			code += "(";
			String delimitar = "";
			for (String key: keySet) {
				Object value = keyValueMap.get(key);
				code += delimitar + key + " = \"" + value.toString() + "\"";
				delimitar = ", ";
			}
			code += ")";
		}
		return code;			
	}
}
