package models.controlFlowModel;

import models.dataFlowModel.DataTransferChannelGenerator;

/*************************************************************
 * An object is mapped to I/O channel.
 * ToDo: Fecthing name from <Channel-Generator> in Data-Flow-Graph
 */
public class EntryPointObjectNode extends ObjectNode {
	DataTransferChannelGenerator ioChannelGenerator = null;

	/*************************************************************
	 * [ *constructor ]
	/*************************************************************
	 * 
	 */
	public EntryPointObjectNode(DataTransferChannelGenerator ioChannelGenerator) {
		super(null);
		this.ioChannelGenerator = ioChannelGenerator;
	}
	
	public EntryPointObjectNode(String name, DataTransferChannelGenerator ioChannelGenerator) {
		super(name);
		this.ioChannelGenerator = ioChannelGenerator;
	}
	
	/*************************************************************
	 * [ *public ]
	/*************************************************************
	 * 
	 */
	public DataTransferChannelGenerator getIoChannelGenerator() {
		return ioChannelGenerator;
	}

	public void setIoChannelGenerator(DataTransferChannelGenerator ioChannelGenerator) {
		this.ioChannelGenerator = ioChannelGenerator;
	}
	
}
