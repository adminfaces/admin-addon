package com.github.adminfaces.addon.util;

import com.github.adminfaces.addon.scaffold.metamodel.AdminFacesMetaModelProvider;
import com.github.adminfaces.addon.scaffold.model.ScaffoldEntity;
import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_PERSISTENCE_COORDINATE;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Plugin;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.javaee.jpa.PersistenceMetaModelFacet;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;

import org.jboss.forge.addon.scaffold.util.ScaffoldUtil;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

/**
 * Created by pestano on 20/09/15.
 */
public class AdminScaffoldUtils extends ScaffoldUtil {

    public static final Logger LOG = Logger.getLogger(AdminScaffoldUtils.class.getName());

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
    
    public static boolean isEmbeddedField(FieldSource<JavaClassSource> field) {
        return field.hasAnnotation(Embedded.class) || field.getOrigin().hasAnnotation(Embeddable.class);
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
    
   public static boolean isValidDisplayField(FieldSource<JavaClassSource> field) {
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

    public static void setupAdminPersistence(Project project, DependencyUtil dependencyUtil, FacetFactory facetFactory) {
        addAdminPersistence(project, dependencyUtil);
        addEntityManagerProducer(project);
        configJPAMetaModel(project, facetFactory);
    }

    public static List<FieldSource<JavaClassSource>> resolveToManyAssociationFields(List<FieldSource<JavaClassSource>> fields) {
        List<FieldSource<JavaClassSource>> toManyFields = new ArrayList<>();
        for (FieldSource<JavaClassSource> field : fields) {
            if (AdminScaffoldUtils.hasToManyAssociation(field)) {
                toManyFields.add(field);
            }
        }
        return toManyFields;
    }

    public static List<FieldSource<JavaClassSource>> resolveToOneAssociationFields(List<FieldSource<JavaClassSource>> fields) {
        List<FieldSource<JavaClassSource>> toOneFields = new ArrayList<>();
        for (FieldSource<JavaClassSource> field : fields) {
            if (AdminScaffoldUtils.hasToOneAssociation(field)) {
                toOneFields.add(field);
            }
        }
        return toOneFields;
    }
    
    private static void configJPAMetaModel(Project project, FacetFactory facetFactory) {
        MavenFacet pom = project.getFacet(MavenFacet.class);
        boolean isMetaModelConfigured = false;
        if (pom.getModel().getBuild() == null || pom.getModel().getBuild().getPlugins().isEmpty()) {
            isMetaModelConfigured = false;
        } else {
            Plugin metaModelPlugin = pom.getModel().getBuild().getPluginsAsMap()
                .get("org.bsc.maven:maven-processor-plugin");
            if (metaModelPlugin == null) {
                isMetaModelConfigured = false;
            } else {
                isMetaModelConfigured = true;
            }
        }

        if (!isMetaModelConfigured) {
            Iterable<PersistenceMetaModelFacet> facets = facetFactory.createFacets(project,
                PersistenceMetaModelFacet.class);
            for (PersistenceMetaModelFacet metaModelFacet : facets) {
                metaModelFacet.setMetaModelProvider(new AdminFacesMetaModelProvider());
                if (facetFactory.install(project, metaModelFacet)) {
                    DependencyFacet facet = project.getFacet(DependencyFacet.class);
                    DependencyBuilder jpaModelegenDependency = DependencyBuilder.create().setCoordinate(CoordinateBuilder.create().setGroupId("org.hibernate").setArtifactId("hibernate-jpamodelgen"));
                   /* if(facet.hasDirectDependency(jpaModelegenDependency)) {
                        facet.removeDependency(jpaModelegenDependency);//not needed on direct deps
                    }*/
                    break;
                }
            }
        }

    }
    
    private static void addEntityManagerProducer(Project project) {
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);
        DirectoryResource sourceDirectory = javaSource.getSourceDirectory();
        String emProducerPath = (sourceDirectory.getFullyQualifiedName() +"/" + metadataFacet.getProjectGroupName() + "/infra/EntityManagerProducer").replaceAll("\\.", "/")+".java";
        if(!new File(emProducerPath).exists()) {
            try (InputStream emProducerStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/infra/persistence/EntityManagerProducer.java")) {
                JavaSource<?> entityManagerProducer = (JavaSource<?>) Roaster.parse(emProducerStream);
                entityManagerProducer.setPackage(metadataFacet.getProjectGroupName() + ".infra");
                javaSource.saveJavaSource(entityManagerProducer);
                FileUtils.copyInputStreamToFile(emProducerStream, new File(project.getRoot().getFullyQualifiedName()
                    + entityManagerProducer.getPackage().replaceAll("\\.", "/")));
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Could not add 'EntityManagerProducer'.", e);
            }
        }

    }

    private static void addAdminPersistence(Project project, DependencyUtil dependencyUtil) {
        DependencyBuilder adminPersistenceDependency = DependencyBuilder.create()
            .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_PERSISTENCE_COORDINATE));
        dependencyUtil.installDependency(project.getFacet(DependencyFacet.class), adminPersistenceDependency);
        configDeltaSpike(project);
    }

    private static void configDeltaSpike(Project project) {
        Resource<?> resources = project.getFacet(ResourcesFacet.class).getResourceDirectory();

        if (!resources.getChild("apache-deltaspike.properties").exists()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/apache-deltaspike.properties")) {
                IOUtils.copy(is, new FileOutputStream(
                    new File(resources.getFullyQualifiedName() + "/apache-deltaspike.properties")));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not add 'apache-deltaspike.properties'.", e);
            }
        }
        WebResourcesFacet webResources = project.getFacet(WebResourcesFacet.class);
        FileResource<?> beansXml = webResources.getWebRootDirectory().getChildDirectory("WEB-INF").getChild("beans.xml").reify(FileResource.class);
        Node node = XMLParser.parse(beansXml.getResourceInputStream());
        Node alternativesNode = node.getOrCreate("alternatives");
        Optional<Node> deltaspikeTransactionStrategy = alternativesNode.getChildren().stream()
            .filter(f -> f.getName().equals("class") && f.getText().contains("BeanManagedUserTransactionStrategy"))
            .findFirst();

        if (!deltaspikeTransactionStrategy.isPresent()) {
            alternativesNode.createChild("class")
                .text("org.apache.deltaspike.jpa.impl.transaction.BeanManagedUserTransactionStrategy");
            beansXml.setContents(XMLParser.toXMLInputStream(node));
        }
    }
   
}
