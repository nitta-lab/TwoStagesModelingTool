package models.dataFlowModel;

import models.NodeAttribute;

public class StoreAttribute extends NodeAttribute {
	private boolean isNeeded = false;
	private boolean isStored = false;
	
	public boolean isNeeded() {
		return isNeeded;
	}
	
	public void setNeeded(boolean isNeeded) {
		this.isNeeded = isNeeded;
	}
	
	public boolean isStored() {
		return isStored;
	}
	
	public void setStored(boolean isStored) {
		this.isStored = isStored;
	}
}
