package models.dataFlowModel;

import models.*;

public class DataFlowEdge extends Edge {
	protected DataTransferChannelGenerator channelGenerator = null;

	public DataFlowEdge(ResourceNode src, ResourceNode dst, DataTransferChannelGenerator channelGenerator) {
		super(src, dst);
		this.channelGenerator = channelGenerator;
	}
	
	public DataTransferChannelGenerator getChannelGenerator() {
		return channelGenerator;
	}
	
	public String toString() {
		return channelGenerator.getChannelName();
	}
}
