package com.github.adminfaces.addon.scaffold.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import com.github.adminfaces.addon.util.AdminScaffoldUtils;
import static com.github.adminfaces.addon.util.AdminScaffoldUtils.extractEmbeddedFields;
import static com.github.adminfaces.addon.util.AdminScaffoldUtils.extractEntityFields;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.roaster.Roaster;

public class ScaffoldEntity implements Serializable {

    private static final long serialVersionUID = 2357194690463276781L;

    private final JavaClassSource entity;
    private List<FieldSource<JavaClassSource>> entityFields;
    private final EntityConfig entityConfig;
    private final Project project;
    private List<FieldSource<JavaClassSource>> embeddedFields;

    public ScaffoldEntity(JavaClassSource entity, EntityConfig entityConfig, Project project) {
        this.entity = entity;
        this.entityConfig = entityConfig;
        this.project = project;
    }

    public String getName() {
        return entity.getName();
    }

    public boolean isHidden(FieldSource<JavaClassSource> field) {
        return field.getName().equals("version") || (getFieldConfig(field.getName()) != null && getFieldConfig(field.getName()).isHidden());
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
     * Lists entity fields excluding fields that are not persisted and embedded fields
     *
     * @return
     */
    public List<FieldSource<JavaClassSource>> getFields() {
        if (entityFields == null) {
            entityFields = new ArrayList<>();
            entityFields.addAll(extractEntityFields(entity));
        }
        return entityFields;
    }

    public List<FieldSource<JavaClassSource>> getEmbeddedFields() {
        if (embeddedFields == null) {
            embeddedFields = new ArrayList<>();
            embeddedFields.addAll(extractEmbeddedFields(entity));
        }
        return embeddedFields;
    }

    public List<FieldSource<JavaClassSource>> getFieldsFromEmbeddedField(FieldSource<JavaClassSource> embeddedField) throws FileNotFoundException {
        List<FieldSource<JavaClassSource>> fields = new ArrayList<>();
        String sourceFolder = AdminScaffoldUtils.resolveSourceFolder(project);
        String qualifiedName = embeddedField.getType().getQualifiedName();
        JavaClassSource embeddedFieldClassSource = Roaster.parse(JavaClassSource.class, new File(sourceFolder + "/" + qualifiedName.replace(".", "/") + ".java"));
        embeddedFieldClassSource.getFields().stream()
            .filter(f -> !f.hasAnnotation(Transient.class) && (f.hasAnnotation(Column.class)
            || hasAssociation(f) || f.hasAnnotation(Basic.class)))
            .forEach(fields::add);
        return fields;
    }
    
    /**
     * Used by test dataset (dataset.yml) generation
     * @param associationField
     * @return
     * @throws FileNotFoundException
     */
    public List<FieldSource<JavaClassSource>> getRequiredFieldsFromAssociationField(FieldSource<JavaClassSource> associationField) throws FileNotFoundException {
        List<FieldSource<JavaClassSource>> fields = new ArrayList<>();
        String sourceFolder = AdminScaffoldUtils.resolveSourceFolder(project);
        String qualifiedName = associationField.getType().getQualifiedName();
        JavaClassSource associationFieldClassSource = Roaster.parse(JavaClassSource.class, new File(sourceFolder + "/" + qualifiedName.replace(".", "/") + ".java"));
        associationFieldClassSource.getFields().stream()
            .filter(f -> !f.hasAnnotation(Transient.class) && (f.hasAnnotation(Column.class) || f.hasAnnotation(Basic.class)))
            .filter(f -> AdminScaffoldUtils.resolveRequiredAttribute(f) || f.hasAnnotation(Version.class))
            .forEach(fields::add);
        
        associationFieldClassSource.getFields().stream()
        .filter(f -> f.hasAnnotation(Embedded.class))
        .forEach(f -> {
            try {
                AdminScaffoldUtils.getFieldsFromEmbeddedField(f, project)
                    .stream()
                    .filter(f2 -> AdminScaffoldUtils.resolveRequiredAttribute(f2))
                    .forEach(fields::add);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(AdminScaffoldUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
});
        return fields;
    }

    public String getDisplayField() {
        return entityConfig.getDisplayField();
    }

    /**
     * retrieves the field name of given association
     *
     * This methods traverse association fields looking for a non nullable String field
     *
     * @param associationField a entity field that represents an (JPA) association
     * @return field name to be used as display field
     */
    public String getAssociationDisplayField(FieldSource<JavaClassSource> associationField) {
        JavaClassSource associationClassSource = null;
        String qualifiedName = null;
        if (associationField.getType().isParameterized()) {
            qualifiedName = associationField.getType().getTypeArguments().get(0).getQualifiedName();
        } else {
            qualifiedName = associationField.getType().getQualifiedName();
        }
        try {
            String sourceFolder = AdminScaffoldUtils.resolveSourceFolder(project);
            associationClassSource = Roaster.parse(JavaClassSource.class, new File(sourceFolder + "/" + qualifiedName.replace(".", "/") + ".java"));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ScaffoldEntity.class.getName()).log(Level.SEVERE, null, ex);
        }
        return AdminScaffoldUtils.resolveDisplayField(associationClassSource);
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
    
    public boolean isBidirectionalAssociation(FieldSource<JavaClassSource> field) {
        return AdminScaffoldUtils.isBidirectionalAssociation(field);
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
    
    public boolean isToggleSwitchType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.TOGGLE_SWITCH);
    }

    public boolean isCalendarType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.CALENDAR);
    }

    public boolean isDatePickerType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.DATEPICKER);
    }

    public boolean isTextAreaType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.TEXT_AREA);
    }

    public boolean isSpinnerType(FieldSource<JavaClassSource> field) {
        return getFieldConfig(field).getType().equals(ComponentTypeEnum.SPINNER);
    }
    
    public FieldSource<JavaClassSource> getField(String name) {
    	return entity.getField(name);
    }
    
    public boolean hasVersionField() {
    	return getField("version") != null;
    }

    public boolean isDatatableEditable() {
        return entityConfig.getDatatableEditable();
    }
    
    public boolean isDatatableReflow() {
        return entityConfig.getDatatableReflow();
    }

}