package com.github.adminfaces.addon.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import org.jboss.forge.addon.scaffold.util.ScaffoldUtil;
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

    public static void unzip(InputStream zipFile, String targetDir) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(zipFile)) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                File destPath = new File(targetDir, zipEntry.getName());
                log.info("Unpacking {}.", destPath.getAbsoluteFile());
                if (!zipEntry.isDirectory()) {
                    FileOutputStream fout = new FileOutputStream(destPath);
                    final byte[] buffer = new byte[8192];
                    int n = 0;
                    while (-1 != (n = zipInputStream.read(buffer))) {
                        fout.write(buffer, 0, n);
                    }
                    fout.close();
                } else {
                    destPath.mkdir();
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    public static boolean hasAssociation(FieldSource<JavaClassSource> field) {
        return field.hasAnnotation(OneToMany.class) || field.hasAnnotation(OneToOne.class)
            || field.hasAnnotation(ManyToOne.class) || field.hasAnnotation(ManyToMany.class);
    }

    public static boolean hasToManyAssociation(FieldSource<JavaClassSource> field) {
        return field.hasAnnotation(OneToMany.class) || field.hasAnnotation(ManyToMany.class);
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
}
