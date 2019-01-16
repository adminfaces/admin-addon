package com.github.adminfaces.addon.scaffold.model;

import java.util.ArrayList;
import java.util.List;

public class EntityConfig {
	
	private List<FieldConfig> fields = new ArrayList<>();
	private String mainField;//field used in pages where this entity needs to be printed
	
	public List<FieldConfig> getFields() {
		return fields;
	}

	public String getMainField() {
		return mainField;
	}

	public void setFields(List<FieldConfig> fields) {
		this.fields = fields;
	}

	public void setMainField(String mainField) {
		this.mainField = mainField;
	}
	
	
}
