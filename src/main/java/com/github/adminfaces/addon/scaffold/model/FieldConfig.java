package com.github.adminfaces.addon.scaffold.model;

/**
 *
 * @author rafael-pestano
 *
 * Represents an entity field configuration to be used on pages
 *
 */
public class FieldConfig {

    private String name;
    private Boolean required;
    private Boolean hidden;
    private Integer length;
    private ComponentTypeEnum type;

    public FieldConfig() {
    }

    public FieldConfig(String name, Boolean required, Boolean hidden, Integer length, ComponentTypeEnum type) {
        super();
        this.name = name;
        this.required = required;
        this.hidden = hidden;
        this.length = length;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isRequired() {
        return required;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean isHidden() {
        return hidden;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(Boolean hidden) {
        this.hidden = hidden;
    }

    public ComponentTypeEnum getType() {
        return type;
    }

    public void setType(ComponentTypeEnum type) {
        this.type = type;
    }

    public String getTypeLower() {
        return type.name().toLowerCase();
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

}
