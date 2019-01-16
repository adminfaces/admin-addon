package com.github.adminfaces.addon.scaffold.model;

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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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
			throw new RuntimeException("Could not load entity config from:" + entityConfigFile.getFullyQualifiedName(),e);
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

		for (FieldSource<JavaClassSource> field: entityFields) {
			boolean required = resolveRequiredAttribute(field);
			ComponentTypeEnum type = resolveComponentType(field);
			Integer length = resolveLengthAttribute(field);
			entityConfig.getFields().add(new FieldConfig(field.getName(), required, false, length, type));
		}
		entityConfigFile.setContents(new Yaml().dump(entity));
		return entityConfig;
	}

	private static Integer resolveLengthAttribute(FieldSource<JavaClassSource> field) {
		AnnotationSource<JavaClassSource> collumnAnnotation = field.getAnnotation(Column.class);
		Integer length = 30;
		if(collumnAnnotation != null && collumnAnnotation.getStringValue("length") != null ) {
			length = Integer.parseInt(collumnAnnotation.getStringValue("length"));
		}
		return length;
	}

	private static ComponentTypeEnum resolveComponentType(FieldSource<JavaClassSource> field) {
		if(field.hasAnnotation(Temporal.class)) {
			return ComponentTypeEnum.CALENDAR;
		}
		if(AdminScaffoldUtils.hasToManyAssociation(field)) {
			return ComponentTypeEnum.CHECKBOXMENU;
		}
		
		if(AdminScaffoldUtils.hasToOneAssociation(field)) {
			return ComponentTypeEnum.SELECT_ONE_MENU;
		}
	    Type<JavaClassSource> type = field.getType(); 
	    if(type.isType(String.class)) {
	    	return ComponentTypeEnum.INPUT_TEXT;
	    }
	    if(type.isType(Long.class) || type.isType(Integer.class) || type.isType(Double.class) || type.isType(BigDecimal.class)) {
	    	return ComponentTypeEnum.INPUT_NUMBER;
	    }
	    //TODO inspect other fields
	    
		return ComponentTypeEnum.INPUT_TEXT;
	}
	
	private static boolean resolveRequiredAttribute(FieldSource<JavaClassSource> field) {
		boolean required = false;
		AnnotationSource<JavaClassSource> collumnAnnotation = field.getAnnotation(Column.class);
		if(field.hasAnnotation(NotNull.class) || (collumnAnnotation != null && collumnAnnotation.getStringValue("nullable").equals("false"))) {
			required = true;
		} else if(AdminScaffoldUtils.hasAssociation(field)) {
			if(field.hasAnnotation(OneToOne.class) && "false".equals(field.getAnnotation(OneToOne.class).getStringValue("optional"))) {
				required = true;
			}
		}
		
		return required;
	}

 

}
