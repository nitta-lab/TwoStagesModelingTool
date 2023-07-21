package models.algebra;

import java.util.ArrayList;
import java.util.List;

public class Type {
	private String typeName;
	private String implementationTypeName;
	private String interfaceTypeName;
	private List<Type> parentTypes = new ArrayList<>();
	
	public Type(String typeName, String implementastionTypeName) {
		this.typeName = typeName;
		this.implementationTypeName = implementastionTypeName;
		this.interfaceTypeName = implementastionTypeName;
	}
	
	public Type(String typeName, String implementastionTypeName, String interfaceTypeName) {
		this.typeName = typeName;
		this.implementationTypeName = implementastionTypeName;
		this.interfaceTypeName = interfaceTypeName;
	}

	public Type(String typeName, String implementastionTypeName, Type parentType) {
		this.typeName = typeName;
		this.implementationTypeName = implementastionTypeName;
		this.interfaceTypeName = implementastionTypeName;
		this.parentTypes.add(parentType);
	}
	
	public Type(String typeName, String implementastionTypeName, String interfaceTypeName, Type parentType) {
		this.typeName = typeName;
		this.implementationTypeName = implementastionTypeName;
		this.interfaceTypeName = interfaceTypeName;
		this.parentTypes.add(parentType);
	}

	public String getTypeName() {
		return typeName;
	}
	
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	
	public String getImplementationTypeName() {
		return implementationTypeName;
	}
	
	public void setImplementationTypeName(String implementastionTypeName) {
		this.implementationTypeName = implementastionTypeName;
	}
	
	public String getInterfaceTypeName() {
		return interfaceTypeName;
	}
	
	public void setInterfaceTypeName(String interfaceTypeName) {
		this.interfaceTypeName = interfaceTypeName;
	}
	
	public List<Type> getParentTypes() {
		return parentTypes;
	}

	public void addParentType(Type parentType) {
		parentTypes.add(parentType);
	}

	public void replaceParentType(Type oldParentType, Type newParentType) {
		parentTypes.set(parentTypes.indexOf(oldParentType), newParentType);
	}
	
	public boolean isAncestorOf(Type another) {
		if (this.equals(another)) return true;
		if (another.getParentTypes() == null) return false;
		for (Type anothersParentType: another.getParentTypes()) {
			if (isAncestorOf(anothersParentType)) return true;
		}
		return false;
	}

	public Memento createMemento() {
		return new Memento(implementationTypeName, interfaceTypeName, parentTypes);
	}
	
	public void setMemento(Memento memento) {
		this.implementationTypeName = memento.implementationTypeName;
		this.interfaceTypeName = memento.interfaceTypeName;
		this.parentTypes = memento.parentTypes;
	}
	
	public static class Memento {
		private String implementationTypeName;
		private String interfaceTypeName;
		private List<Type> parentTypes;
		
		public Memento(String implementationTypeName, String interfaceTypeName, List<Type> parentTypes) {
			this.implementationTypeName = implementationTypeName;
			this.interfaceTypeName = interfaceTypeName;
			this.parentTypes = parentTypes;
		}
	}
}
