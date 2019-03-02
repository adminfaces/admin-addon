package com.github.adminfaces.addon.scaffold.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

public class EntityConfig {

    private List<FieldConfig> fields = new ArrayList<>();
    private String displayField;// field used in pages where this entity needs to be printed
    private String menuIcon;//font awesome icon to be used on the menu entry for this entity
    private Boolean datatableEditable; //list page datatable should be editable directly on the row?
    private Boolean datatableReflow; //list page datatable should reflow?
    private transient Map<String, FieldConfig> fieldConfigMap;
    
    // key is a entity field which is an (JPA) association, value is the association field name to be used as a display field
    private final transient Map<FieldSource<JavaClassSource>, String> associationDisplayFieldMap = new HashMap<>();

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

    public Boolean getDatatableEditable() {
        return datatableEditable;
    }

    public void setDatatableEditable(Boolean datatableEditable) {
        this.datatableEditable = datatableEditable;
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

    public Boolean getDatatableReflow() {
        return datatableReflow;
    }

    public void setDatatableReflow(Boolean datatableReflow) {
        this.datatableReflow = datatableReflow;
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

    public Map<FieldSource<JavaClassSource>, String> getAssociationDisplayFieldMap() {
        return associationDisplayFieldMap;
    }

}
