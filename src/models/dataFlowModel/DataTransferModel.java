package models.dataFlowModel;

import java.util.Set;

import models.dataConstraintModel.ChannelGenerator;
import models.dataConstraintModel.DataConstraintModel;
import models.dataConstraintModel.IdentifierTemplate;

public class DataTransferModel extends DataConstraintModel {	
	public DataFlowGraph getDataFlowGraph() {
		DataFlowGraph dataFlowGraph = new DataFlowGraph();
		for (ChannelGenerator channelGen: getChannelGenerators()) {
			DataTransferChannelGenerator dfChannelGen = (DataTransferChannelGenerator)channelGen;
			Set<IdentifierTemplate> inputResources = dfChannelGen.getInputIdentifierTemplates();
			Set<IdentifierTemplate> outputResources = dfChannelGen.getOutputIdentifierTemplates();
			for (IdentifierTemplate in: inputResources) {
				for (IdentifierTemplate out: outputResources) {
					dataFlowGraph.addEdge(in ,out, dfChannelGen);
				}
			}
		}
		for (ChannelGenerator channelGen: getIOChannelGenerators()) {
			DataTransferChannelGenerator dfChannelGen = (DataTransferChannelGenerator)channelGen;
			Set<IdentifierTemplate> outputResources = dfChannelGen.getOutputIdentifierTemplates();
			for (IdentifierTemplate out: outputResources) {
				dataFlowGraph.addNode(out);
			}			
		}
		return dataFlowGraph;
	}
}
