package com.github.adminfaces.addon.scaffold.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import com.github.adminfaces.addon.util.AdminScaffoldUtils;

public class ScaffoldEntity implements Serializable {

    private static final long serialVersionUID = 2357194690463276781L;

    private final JavaClassSource entity;
    private List<FieldSource<JavaClassSource>> entityFields;
    private final EntityConfig entityConfig;

    public ScaffoldEntity(JavaClassSource entity, EntityConfig entityConfig) {
        this.entity = entity;
        this.entityConfig = entityConfig;
    }

    public String getName() {
        return entity.getName();
    }

    public boolean isHidden(FieldSource<JavaClassSource> field) {
        return field.getName().equals("version") || getFieldConfig(field.getName()).isHidden();
    }

    /**
     * @param fieldName the entity field
     * @return UI configuration for the given field
     */
    public FieldConfig getFieldConfig(String fieldName) {
        return entityConfig.getFieldConfigByName(fieldName);
    }

    public EntityConfig getEntityConfig() {
        return entityConfig;
    }
    

    /**
     * Lists entity fields excluding fields that are not persisted
     *
     * @return
     */
    public List<FieldSource<JavaClassSource>> getFields() {
        if (entityFields == null) {
            entityFields = new ArrayList<>();
            entity.getFields().stream()
                .filter(f -> !f.hasAnnotation(Transient.class) && (f.hasAnnotation(Column.class)
                || hasAssociation(f) || f.hasAnnotation(Basic.class) || f.hasAnnotation(Embedded.class)
                || f.hasAnnotation(Id.class) || f.hasAnnotation(EmbeddedId.class)))
                .forEach(entityFields::add);
        }
        return entityFields;
    }
    
    /**
     * retrieves the field name of given association
     * 
     * This methods traverse association fields looking for a non nullable String field
     * 
     * @param associationField a entity field that represents an (JPA) association
     * @return field name to be used as display field
     */
    public String getAssociationDisplayField(FieldSource<JavaClassSource> field) {
        return AdminScaffoldUtils.resolveDisplayField(field.getType().isParameterized() ? field.getType().getTypeArguments().get(0).getOrigin() : field.getType().getOrigin());
    }

    public String getQualifiedName() {
        return entity.getQualifiedName();
    }

    public boolean isRequired(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field.getName()).isRequired();
    }
    
    public int getSize(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field.getName()).getLength();
    }

    public boolean hasAssociation(FieldSource<JavaClassSource> field) {//just expose to freemarker
        return AdminScaffoldUtils.hasAssociation(field);
    }

    public boolean hasToManyAssociation(FieldSource<JavaClassSource> field) { //just expose to freemarker
        return AdminScaffoldUtils.hasToManyAssociation(field);
    }

    public boolean hasToOneAssociation(FieldSource<JavaClassSource> field) { //just expose to freemarker
        return AdminScaffoldUtils.hasToOneAssociation(field);
    }

    public Type<JavaClassSource> getArrayType(FieldSource<JavaClassSource> field) { //just expose to freemarker
        return AdminScaffoldUtils.getArrayType(field);
    }

    // supported field types
    public FieldConfig getFieldConfig(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field.getName());
    }

    public boolean isAutoCompleteType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.AUTOCOMPLETE);
    }

    public boolean isInputTextType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.INPUT_TEXT);
    }

    public boolean isInputNumberType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.INPUT_NUMBER);
    }

    public boolean isSelectOneMenuType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.SELECT_ONE_MENU);
    }

    public boolean isSelectManyMenuType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.SELECT_MANY_MENU);
    }

    public boolean isSelectManyCheckboxType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.SELECT_MANY_CHECKBOX);
    }

    public boolean isSelectOneRadioType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.SELECT_ONE_RADIO);
    }

    public boolean isCheckboxMenuType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.CHECKBOXMENU);
    }

    public boolean isPasswordType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.PASSWORD);
    }

    public boolean isInputSwitchType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.INPUT_SWITCH);
    }

    public boolean isCalendarType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.CALENDAR);
    }

    public boolean isTextAreaType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.TEXT_AREA);
    }

    public boolean isSpinnerType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.SPINNER);
    }

}
