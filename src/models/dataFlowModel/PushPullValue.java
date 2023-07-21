package models.dataFlowModel;

public enum PushPullValue {
	PULL,
	PUSHorPULL,
	PUSH;
	
	public String toString() {
		switch (this) {
		case PUSHorPULL:
			return "PUSH/PULL";
		case PUSH:
			return "PUSH";
		case PULL:
			return "PULL";
		default:
			return "";				
		}
	}
}
