package com.github.adminfaces.addon.scaffold.model;

import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.*;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.Transient;

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
        entity.getFields().stream()
            .filter(f -> f.hasAnnotation(Column.class) || AdminScaffoldUtils.hasAssociation(f) 
                || f.hasAnnotation(Basic.class) || f.hasAnnotation(Transient.class) 
                || f.hasAnnotation(Embedded.class) || f.hasAnnotation(Id.class) 
                || f.hasAnnotation(EmbeddedId.class))
            .forEach(f -> {
                boolean required = AdminScaffoldUtils.resolveRequiredAttribute(f);
                Integer length = resolveLengthAttribute(f);
                ComponentTypeEnum type = resolveComponentType(f, length);
                entityConfig.getFields().add(new FieldConfig(f.getName(), required, false, length, type));
                if (entityConfig.getDisplayField() == null && type.equals(INPUT_TEXT) && required) { //by default displayField is the first non null inputText field
                    entityConfig.setDisplayField(f.getName());
                }
            });
        if(entityConfig.getDisplayField() == null) {
        	entityConfig.setDisplayField("");//this means we'll use entity's toString() method to display entity on pages
        }
        entityConfig.setMenuIcon("fa fa-circle-o");
        entityConfigFile.setContents(new Yaml().dump(entityConfig));
        return entityConfig;
    }

    private static Integer resolveLengthAttribute(FieldSource<JavaClassSource> field) {
        AnnotationSource<JavaClassSource> columnAnnotation = field.getAnnotation(Column.class);
        Integer length = 50;
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
            return AUTOCOMPLETE;
        }
        Type<JavaClassSource> type = field.getType();
        if (type.isType(String.class)) {
            if (field.getName().toLowerCase().contains("password")) {
                return PASSWORD;
            }
            if (length > 50) {
                return TEXT_AREA;
            } else {
                return INPUT_TEXT;
            }
        }
        if (type.isType(Long.class) || type.isType(Integer.class) || type.isType(Double.class) || type.isType(BigDecimal.class)
            || type.isType("long") || type.isType("int") || type.isType("double")) {
            return INPUT_NUMBER;
        }
        if (type.isType(Boolean.class) || type.isType("double")) {
             return INPUT_SWITCH;
        }     
        //TODO inspect other fields types

        return INPUT_TEXT;
    }

    

}
