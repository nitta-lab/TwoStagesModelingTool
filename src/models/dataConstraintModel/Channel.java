package models.dataConstraintModel;

import java.util.HashSet;
import java.util.Set;

import models.algebra.Variable;

public class Channel {
	protected String channelName;
	protected Set<Selector> selectors = null;
	protected Set<ChannelMember> channelMembers = null;
	protected String sourceText = null;
	
	public Channel(String channelName) {
		this.channelName = channelName;
		selectors = new HashSet<>();
		channelMembers = new HashSet<>();
	}
	
	public Channel(String channelName, Set<Variable> variables) {
		this.channelName = channelName;
		selectors = new HashSet<>();
		for (Variable var: variables) {
			selectors.add(new Selector(var));
		}
		channelMembers = new HashSet<>();
	}

	public String getChannelName() {
		return channelName;
	}

	public Set<Selector> getChannelSelectors() {
		return selectors;
	}

	public void setChannelSelectors(Set<Selector> selectors) {
		this.selectors = selectors;
	}

	public void addSelector(Selector selector) {
		selectors.add(selector);
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
	
	public void removeChannelMember(ResourcePath id) {
		for (ChannelMember cm: channelMembers) {
			if (cm.getResource() == id) {
				channelMembers.remove(cm);
				break;
			}
		}
	}

	public Set<ResourcePath> getResources() {
		Set<ResourcePath> resources = new HashSet<>();
		for (ChannelMember member: channelMembers) {
			resources.add(member.getResource());
		}
		return resources;
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
