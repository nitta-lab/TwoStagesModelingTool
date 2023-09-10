package models.dataConstraintModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import models.algebra.Type;

public class JsonType extends Type {
	protected boolean isListOrMap = false;
	protected Map<String, Type> memberTypes = null;
	protected Type listOrMapElementType = null;
	
	public JsonType(String typeName, String implementationTypeName) {
		super(typeName, implementationTypeName);
		memberTypes = new HashMap<>();
	}
	
	public JsonType(String typeName, String implementationTypeName, String interfaceTypeName) {
		super(typeName, implementationTypeName, interfaceTypeName);
		memberTypes = new HashMap<>();
	}

	public JsonType(String typeName, String implementationTypeName, Type parentType) {
		super(typeName, implementationTypeName, parentType);
		memberTypes = new HashMap<>();
	}
	
	public JsonType(String typeName, String implementationTypeName, String interfaceTypeName, Type parentType) {
		super(typeName, implementationTypeName, interfaceTypeName, parentType);
		memberTypes = new HashMap<>();
	}
	
	public Map<String, Type> getMemberTypes() {
		return memberTypes;
	}
	
	public Type getMemberType(String key) {
		return memberTypes.get(key);
	}
	
	public Set<String> getKeys() {
		return memberTypes.keySet();
	}
	
	public void addMemberType(String key, Type valueType) {
		memberTypes.put(key, valueType);
	}

//	public boolean isListOrMap() {
//		return isListOrMap;
//	}
//
//	public void setListOrMap(boolean isListOrMap) {
//		this.isListOrMap = isListOrMap;
//	}

	public Type getElementType() {
		return listOrMapElementType;
	}

	public void setElementType(Type listElementType) {
		this.listOrMapElementType = listElementType;
	}
}
