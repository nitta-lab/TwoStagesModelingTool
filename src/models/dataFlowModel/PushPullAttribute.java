package models.dataFlowModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import models.EdgeAttribute;

public class PushPullAttribute extends EdgeAttribute {
	private List<PushPullValue> options;
	
	public PushPullAttribute() {
		options = new ArrayList<>();
	}
	
	public PushPullAttribute(PushPullValue[] options) {	
		this.options = new ArrayList<>(Arrays.asList(options));
	}

	public List<PushPullValue> getOptions() {
		return options;
	}

	public void setOptions(List<PushPullValue> options) {
		this.options = options;
	}

	public void addOption(PushPullValue option) {
		options.add(option);
	}

	public void removeOption(PushPullValue option) {
		options.remove(option);
	}
	
	public void intersectOptions(List<PushPullValue> options) {
		this.options.retainAll(options);
	}
	
	public String[] getOptionStrings() {
		String[] optionString = new String[options.size()];
		for (int i = 0; i < options.size(); i++) {
			optionString[i] = options.get(i).toString();
		}
		return optionString;
	}
	
	public String toString() {
		if (options == null || options.size() == 0) return "";
		return options.get(0).toString();
	}
}
