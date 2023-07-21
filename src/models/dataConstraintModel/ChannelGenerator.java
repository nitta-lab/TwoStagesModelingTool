package models.dataConstraintModel;

import java.util.HashSet;
import java.util.Set;

public class ChannelGenerator {
	protected String channelName;
	protected Set<GroupSelector> groupSelectors = null;
	protected Set<ChannelSelector> channelSelectors = null;
	protected Set<ChannelMember> channelMembers = null;
	protected String sourceText = null;
	
	public ChannelGenerator(String channelName) {
		this.channelName = channelName;
		groupSelectors = new HashSet<>();
		channelSelectors = new HashSet<>();
		channelMembers = new HashSet<>();
	}	

	public String getChannelName() {
		return channelName;
	}

	public Set<GroupSelector> getGroupSelectors() {
		return groupSelectors;
	}

	public void setGroupSelectors(Set<GroupSelector> groupSelectors) {
		this.groupSelectors = groupSelectors;
	}
	
	public void addGroupSelector(GroupSelector groupSelector) {
		groupSelectors.add(groupSelector);
	}
	
	public Set<ChannelSelector> getChannelSelectors() {
		return channelSelectors;
	}

	public void setChannelSelectors(Set<ChannelSelector> channelSelectors) {
		this.channelSelectors = channelSelectors;
	}

	public void addChannelSelector(ChannelSelector channelSelector) {
		channelSelectors.add(channelSelector);
	}
	
	public void addSelector(Selector selector) {
		if (selector instanceof GroupSelector) {
			groupSelectors.add((GroupSelector)selector);			
		} else if (selector instanceof ChannelSelector) {
			channelSelectors.add((ChannelSelector)selector);			
		}
	}

	public Set<ChannelMember> getChannelMembers() {
		return channelMembers;
	}

	public void setChannelMembers(Set<ChannelMember> channelMembers) {
		this.channelMembers = channelMembers;
		for (ChannelMember channelMember: channelMembers) {
			for (Selector selector: channelMember.getSelectors()) {
				addSelector(selector);
			}
		}
	}
	
	public void addChannelMember(ChannelMember channelMember) {
		channelMembers.add(channelMember);
		for (Selector selector: channelMember.getSelectors()) {
			addSelector(selector);
		}
	}
	
	public void removeChannelMember(IdentifierTemplate id) {
		for (ChannelMember cm: channelMembers) {
			if (cm.getIdentifierTemplate() == id) {
				channelMembers.remove(cm);
				break;
			}
		}
	}

	public Set<IdentifierTemplate> getIdentifierTemplates() {
		Set<IdentifierTemplate> identifierTemplates = new HashSet<>();
		for (ChannelMember member: channelMembers) {
			identifierTemplates.add(member.getIdentifierTemplate());
		}
		return identifierTemplates;
	}
	
	public String toString() {
		return channelName;
	}
	
	public void setSourceText(String sourceText) {
		this.sourceText = sourceText;
	}
	
	public String getSourceText() {
		if (sourceText == null) {
			return toString();
		}
		return sourceText;
	}
}
