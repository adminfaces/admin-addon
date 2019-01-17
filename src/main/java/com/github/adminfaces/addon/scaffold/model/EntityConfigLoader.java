package com.github.adminfaces.addon.scaffold.model;

import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.*;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.yaml.snakeyaml.Yaml;

import com.github.adminfaces.addon.util.AdminScaffoldUtils;

public class EntityConfigLoader {

    public static EntityConfig createOrLoadEntityConfig(JavaClassSource entity, Project project) {
        DirectoryResource scaffoldDir = project.getFacet(ResourcesFacet.class).getResourceDirectory()
            .getChildDirectory("scaffold");
        FileResource<?> entityConfigFile = (FileResource<?>) scaffoldDir.getChild(entity.getName() + ".yml");
        EntityConfig entityConfig = null;
        if (!entityConfigFile.exists()) {
            entityConfig = createEntityConfig(entity, entityConfigFile);
        } else {
            entityConfig = loadEntityConfig(entityConfigFile);
        }

        return entityConfig;
    }

    private static EntityConfig loadEntityConfig(FileResource<?> entityConfigFile) {
        try (InputStream entityConfigStream = entityConfigFile.getResourceInputStream()) {
            return new Yaml().loadAs(entityConfigStream, EntityConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Could not load entity config from:" + entityConfigFile.getFullyQualifiedName(), e);
        }
    }

    private static EntityConfig createEntityConfig(JavaClassSource entity, FileResource<?> entityConfigFile) {
        EntityConfig entityConfig = new EntityConfig();
        List<FieldSource<JavaClassSource>> entityFields = new ArrayList<>();
        entity.getFields().stream()
            .filter(f -> f.hasAnnotation(Column.class) || AdminScaffoldUtils.hasAssociation(f) || f.hasAnnotation(Basic.class)
            || f.hasAnnotation(Transient.class) || f.hasAnnotation(Embedded.class)
            || f.hasAnnotation(Id.class) || f.hasAnnotation(EmbeddedId.class))
            .forEach(entityFields::add);

        for (FieldSource<JavaClassSource> field : entityFields) {
            boolean required = resolveRequiredAttribute(field);
            Integer length = resolveLengthAttribute(field);
            ComponentTypeEnum type = resolveComponentType(field, length);
            entityConfig.getFields().add(new FieldConfig(field.getName(), required, false, length, type));
            if(entityConfig.getMainField() == null && type.equals(INPUT_TEXT) && required) { //by default mainsField is the first non null inputText field
                entityConfig.setMainField(field.getName());
            }
        }
        entityConfigFile.setContents(new Yaml().dump(entityConfig));
        return entityConfig;
    }

    private static Integer resolveLengthAttribute(FieldSource<JavaClassSource> field) {
        AnnotationSource<JavaClassSource> columnAnnotation = field.getAnnotation(Column.class);
        Integer length = 30;
        if (columnAnnotation != null && columnAnnotation.getStringValue("length") != null) {
            length = Integer.parseInt(columnAnnotation.getStringValue("length"));
        }
        return length;
    }

    private static ComponentTypeEnum resolveComponentType(FieldSource<JavaClassSource> field, Integer length) {
        if (field.hasAnnotation(Temporal.class)) {
            return ComponentTypeEnum.CALENDAR;
        }
        if (AdminScaffoldUtils.hasToManyAssociation(field)) {
            return ComponentTypeEnum.CHECKBOXMENU;
        }

        if (AdminScaffoldUtils.hasToOneAssociation(field)) {
            return SELECT_ONE_MENU;
        }
        Type<JavaClassSource> type = field.getType();
        if (type.isType(String.class)) {
        	if(field.getName().toLowerCase().contains("password")) {
        		return PASSWORD;
        	}
        	if(length > 30) {
        		return TEXT_AREA;
        	} else {
        		return INPUT_TEXT;
        	}
        }
        if (type.isType(Long.class) || type.isType(Integer.class) || type.isType(Double.class) || type.isType(BigDecimal.class) ||
        		type.isType("long") || type.isType("int") || type.isType("double")) {
            return INPUT_NUMBER;
        }
        //TODO inspect other fields types

        return INPUT_TEXT;
    }

    private static boolean resolveRequiredAttribute(FieldSource<JavaClassSource> field) {
        boolean required = false;
        AnnotationSource<JavaClassSource> collumnAnnotation = field.getAnnotation(Column.class);
        if (field.hasAnnotation(NotNull.class) || (collumnAnnotation != null && "false".equals(collumnAnnotation.getStringValue("nullable")))) {
            required = true;
        } else if (AdminScaffoldUtils.hasAssociation(field)) {
            if (field.hasAnnotation(OneToOne.class) && "false".equals(field.getAnnotation(OneToOne.class).getStringValue("optional"))) {
                required = true;
            }
        }

        return required;
    }

}
