package com.github.adminfaces.addon.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;

import org.jboss.forge.addon.scaffold.util.ScaffoldUtil;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pestano on 20/09/15.
 */
public class AdminScaffoldUtils extends ScaffoldUtil {

    public static final Logger log = LoggerFactory.getLogger(AdminScaffoldUtils.class.getName());

    public static boolean hasAssociation(FieldSource<JavaClassSource> field) {
        return field.hasAnnotation(OneToMany.class) || field.hasAnnotation(OneToOne.class)
            || field.hasAnnotation(ManyToOne.class) || field.hasAnnotation(ManyToMany.class);
    }

    public static boolean hasToManyAssociation(FieldSource<JavaClassSource> field) {
        return field.hasAnnotation(OneToMany.class) || field.hasAnnotation(ManyToMany.class);
    }
    
    public static boolean isBidirectionalAssociation(FieldSource<JavaClassSource> field) {
        return (field.hasAnnotation(OneToMany.class) && field.getAnnotation(OneToMany.class).getStringValue("mappedBy") != null) 
            || (field.hasAnnotation(ManyToMany.class) && field.getAnnotation(ManyToMany.class).getStringValue("mappedBy") != null);
    }

    public static boolean hasToOneAssociation(FieldSource<JavaClassSource> field) {
        return field.hasAnnotation(OneToOne.class) || field.hasAnnotation(ManyToOne.class);
    }

    public static Type<JavaClassSource> getArrayType(FieldSource<JavaClassSource> field) {
        return field.getType().isParameterized() ? field.getType().getTypeArguments().get(0) : field.getType();
    }

    public static boolean resolveRequiredAttribute(FieldSource<JavaClassSource> field) {
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


    public static String resolveDisplayField(JavaClassSource entity) {
        Optional<FieldSource<JavaClassSource>> displayField = entity.getFields().stream()
            .filter((f -> isValidDisplayField(f) && f.getType().isType(String.class) && AdminScaffoldUtils.resolveRequiredAttribute(f)))
            .findFirst();
        if (displayField.isPresent()) {
            return displayField.get().getName();
        } else {
            return "";
        }
    }
    
   private static boolean isValidDisplayField(FieldSource<JavaClassSource> field) {
       return field.hasAnnotation(Column.class)
            || field.hasAnnotation(Basic.class)
            || field.hasAnnotation(Embedded.class) || field.hasAnnotation(Id.class)
            || field.hasAnnotation(EmbeddedId.class);
   }
   
   
   public static List<FieldSource<JavaClassSource>> extractEntityFields(JavaClassSource entity) {
        List<FieldSource<JavaClassSource>> fields = new ArrayList<>();
        entity.getFields().stream()
            .filter(f -> !f.hasAnnotation(Transient.class) && (f.hasAnnotation(Column.class)
                || hasAssociation(f) || f.hasAnnotation(Basic.class)
                || f.hasAnnotation(Id.class) || f.hasAnnotation(EmbeddedId.class)))
            .forEach(fields::add);
        return fields;
    }
   
    public static List<FieldSource<JavaClassSource>> extractEmbeddedFields(JavaClassSource entity) {
        List<FieldSource<JavaClassSource>> fields = new ArrayList<>();
        entity.getFields().stream()
            .filter(f -> !f.hasAnnotation(Transient.class) && f.hasAnnotation(Embedded.class))
            .forEach(fields::add);
        return fields;
    }
    
    public static List<FieldSource<JavaClassSource>> getFieldsFromEmbeddedField(FieldSource<JavaClassSource> embeddedField, Project project) throws FileNotFoundException {
        List<FieldSource<JavaClassSource>> fields = new ArrayList<>();
        String sourceFolder = resolveSourceFolder(project);
        String qualifiedName = embeddedField.getType().getQualifiedName();
        JavaClassSource embeddedFieldClassSource = Roaster.parse(JavaClassSource.class, new File(sourceFolder + "/" + qualifiedName.replace(".", "/") + ".java"));
        embeddedFieldClassSource.getFields().stream()
            .filter(f -> !f.hasAnnotation(Transient.class) && (f.hasAnnotation(Column.class)
            || hasAssociation(f) || f.hasAnnotation(Basic.class)))
            .forEach(fields::add);
        return fields;
    }

    public static String resolveSourceFolder(Project project) {
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        return sourceFacet.getSourceDirectory().getFullyQualifiedName();
    }
   
}
