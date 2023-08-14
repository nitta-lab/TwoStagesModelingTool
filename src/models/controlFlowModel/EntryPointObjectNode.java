package models.controlFlowModel;

import models.dataFlowModel.DataTransferChannel;

/*************************************************************
 * An object is mapped to I/O channel.
 * ToDo: Fecthing name from <Channel-Generator> in Data-Flow-Graph
 */
public class EntryPointObjectNode extends ObjectNode {
	DataTransferChannel ioChannel = null;

	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 * 
	 */
	public EntryPointObjectNode(DataTransferChannel ioChannel) {
		super(null);
		this.ioChannel = ioChannel;
	}
	
	public EntryPointObjectNode(String name, DataTransferChannel ioChannel) {
		super(name);
		this.ioChannel = ioChannel;
	}
	
	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * 
	 */
	public DataTransferChannel getIOChannel() {
		return ioChannel;
	}

	public void setIOChannel(DataTransferChannel ioChannel) {
		this.ioChannel = ioChannel;
	}
	
}
