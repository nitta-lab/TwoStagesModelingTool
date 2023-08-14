package models.dataFlowModel;

import models.*;

public class DataFlowEdge extends Edge {
	protected DataTransferChannel channel = null;

	public DataFlowEdge(ResourceNode src, ResourceNode dst, DataTransferChannel channel) {
		super(src, dst);
		this.channel = channel;
	}
	
	public DataTransferChannel getChannel() {
		return channel;
	}
	
	public String toString() {
		return channel.getChannelName();
	}
}
