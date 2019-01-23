package com.github.adminfaces.addon.scaffold.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityConfig {

    private List<FieldConfig> fields = new ArrayList<>();
    private String displayField;// field used in pages where this entity needs to be printed
    private String menuIcon;//font awesome icon to be used on the menu entry for this entity

    private transient Map<String, FieldConfig> fieldConfigMap;

    public List<FieldConfig> getFields() {
        return fields;
    }

    public String getDisplayField() {
        return displayField;
    }

    public void setFields(List<FieldConfig> fields) {
        this.fields = fields;
    }

    public void setDisplayField(String mainField) {
        this.displayField = mainField;
    }

    public FieldConfig getFieldConfigByName(String name) {
        return getFieldConfigMap().get(name);
    }

    public String getMenuIcon() {
        return menuIcon;
    }

    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
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
