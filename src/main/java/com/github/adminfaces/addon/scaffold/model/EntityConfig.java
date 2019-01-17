package com.github.adminfaces.addon.scaffold.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityConfig {

	private List<FieldConfig> fields = new ArrayList<>();
	private String mainField;// field used in pages where this entity needs to be printed
	private transient Map<String, FieldConfig> fieldConfigMap;

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
	
	public FieldConfig getFieldConfigByName(String name) {
		return getFieldConfigMap().get(name);
	}

	public Map<String, FieldConfig> getFieldConfigMap() {
		if (fieldConfigMap == null) {
			fieldConfigMap = new HashMap<>();
			if (fields != null) {
				for (FieldConfig fieldConfig : fields) {
					fieldConfigMap.put(fieldConfig.getName(), fieldConfig);
				}
			}
		}
		return fieldConfigMap;
	}

}
