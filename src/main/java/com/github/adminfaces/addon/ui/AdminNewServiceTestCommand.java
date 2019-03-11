package com.github.adminfaces.addon.ui;

/**
 * @author rmpestano
 */
import com.github.adminfaces.addon.facet.AdminFacesTestHarnessFacet;
import com.github.adminfaces.addon.freemarker.FreemarkerTemplateProcessor;
import com.github.adminfaces.addon.freemarker.GenerateDataSetValueFromField;
import com.github.adminfaces.addon.freemarker.TemplateFactory;
import com.github.adminfaces.addon.scaffold.config.ScaffoldConfigLoader;
import com.github.adminfaces.addon.scaffold.model.EntityConfig;
import com.github.adminfaces.addon.scaffold.model.ScaffoldEntity;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.metawidget.util.simple.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.adminfaces.addon.util.AdminScaffoldUtils.*;
import java.util.Optional;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.roaster.model.util.Types;

@FacetConstraint(AdminFacesTestHarnessFacet.class)
public class AdminNewServiceTestCommand extends AbstractProjectCommand {

    private static final Logger LOG = Logger.getLogger(AdminNewServiceTestCommand.class.getName());

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private TemplateFactory templates;

    @Inject
    @WithAttributes(label = "Target services", description = "Select services to create the integration tests.", required = true)
    private UISelectMany<JavaClassSource> targetServices;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass()).name("AdminFaces: New service test").category(Categories.create("AdminFaces"))
            .description("Creates integration tests for AdminFaces services.");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        UIContext context = builder.getUIContext();
        Project project = getSelectedProject(context);
        final List<JavaClassSource> services = getAllServices(project);
        targetServices.setValueChoices(services);
        targetServices.setItemLabelConverter((JavaClassSource source)
            -> source.getQualifiedName());
        builder.add(targetServices);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final Project project = getSelectedProject(context) != null ? getSelectedProject(context)
            : getSelectedProject(context.getUIContext());

        List<Result> results = new ArrayList<>();
        List<Resource<?>> generatedResources = new ArrayList<>();
        results.add(Results.success("Service test(s) created successfully!"));
        for (JavaClassSource service : targetServices.getValue()) {
            ScaffoldEntity scaffoldEntity = resolveServiceEntity(service, project);
            String ccEntity = StringUtils.decapitalize(scaffoldEntity.getName());
            String ccService = StringUtils.decapitalize(service.getName());
            Map<Object, Object> freemarkerContext = new HashMap<>();
            freemarkerContext.put("entity", scaffoldEntity);
            freemarkerContext.put("service", service);
            freemarkerContext.put("ccService", ccService);
            freemarkerContext.put("ccEntity", ccEntity);
            freemarkerContext.put("requiredFields", extractEntityRequiredFields(scaffoldEntity, project));
            freemarkerContext.put("datasetValue", new GenerateDataSetValueFromField());
            freemarkerContext.put("toOneFields", resolveToOneAssociationFields(scaffoldEntity.getFields()));
            createServiceTest(freemarkerContext, service, project, generatedResources);
            createTestDataSet(freemarkerContext, project, generatedResources);
        }

        for (Resource<?> resource : generatedResources) {
            results.add(Results.success("Added " + resource.getFullyQualifiedName().replace(project.getRoot().getFullyQualifiedName(), "")));
        }
        return Results.aggregate(results);
    }

    private void createServiceTest(Map<Object, Object> context, JavaClassSource service, Project project, List<Resource<?>> generatedResources) {
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        JavaClassSource serviceTest = Roaster.parse(JavaClassSource.class,
            FreemarkerTemplateProcessor.processTemplate(context, templates.getServiceTestTemplate()));
        serviceTest.setPackage(service.getPackage());
        JavaResource javaResource = java.getTestJavaResource(serviceTest);
        generatedResources.add(createOrOverwrite(javaResource, serviceTest.toUnformattedString()));
    }

    private void createTestDataSet(Map<Object, Object> context, Project project, List<Resource<?>> generatedResources) {
        String dataset = FreemarkerTemplateProcessor.processTemplate(context, templates.getDataSetTemplate());
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        Resource<?> datasetsDir = resourcesFacet.getTestResourceDirectory()
            .getChild("datasets");
        FileResource<?> datasetFile = datasetsDir.getChild(context.get("ccEntity") + ".yml").reify(FileResource.class);
        generatedResources.add(createOrOverwrite(datasetFile, dataset));
    }

    private ScaffoldEntity resolveServiceEntity(JavaClassSource service, Project project) throws FileNotFoundException {
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        String firstType = Types.splitGenerics(service.getSuperType())[0];
        String entityQualifiedName = service.resolveType(firstType);
        String sourceFolder = sourceFacet.getSourceDirectory().getFullyQualifiedName();
        try {
            JavaClassSource entity = Roaster.parse(JavaClassSource.class, new File(sourceFolder + "/" + entityQualifiedName.replace(".", "/") + ".java"));
            addEntityClassToPersistenceXml(entityQualifiedName, project);
            EntityConfig entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
            return new ScaffoldEntity(entity, entityConfig, project);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, String.format("Could not extract entity from service %s ", service.getQualifiedName()), e);
            throw new RuntimeException("Could not resolve entity from service " + service.getQualifiedName(), e);
        }
    }

    public List<JavaClassSource> getAllServices(Project project) {
        final List<JavaClassSource> result = new ArrayList<>();
        JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
        List<String> currentServiceTests = new ArrayList<>();

        javaSourceFacet.visitJavaTestSources(new JavaResourceVisitor() {
            @Override
            public void visit(VisitContext context, JavaResource javaResource) {
                if (javaResource.getName().endsWith("It.java")) {
                    currentServiceTests.add(javaResource.getName());
                }
            }
        });
        javaSourceFacet.visitJavaSources(new JavaResourceVisitor() {
            @Override
            public void visit(VisitContext context, JavaResource resource) {
                try {
                    JavaType<?> type = resource.getJavaType();
                    if (type.isClass() && type.hasAnnotation(Stateless.class) && !currentServiceTests.contains(type.getName() + "It")) {
                        JavaClassSource classSource = (JavaClassSource) type;
                        if (classSource.hasImport("com.github.adminfaces.persistence.service.CrudService") || classSource.hasMethodSignature("configRestrictions", "com.github.adminfaces.persistence.model.Filter")) {
                            result.add((JavaClassSource) type);
                        }
                    }
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        return result;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    private void addEntityClassToPersistenceXml(String entityQualifiedName, Project project) {
        DirectoryResource resourceDirectory = project.getFacet(ResourcesFacet.class).getTestResourceDirectory();
        FileResource persistenceXMLFile = resourceDirectory.getChildDirectory("META-INF").getChild("persistence.xml").reify(FileResource.class);
        Node node = XMLParser.parse(persistenceXMLFile.getResourceInputStream());

        Node persistenceUnitNode = node.getSingle("persistence-unit");
        Optional<Node> entityClassNode = persistenceUnitNode
            .getChildren().stream()
            .filter(n -> n.getName().equals("class") && n.getText().equals(entityQualifiedName))
            .findFirst();

        if (!entityClassNode.isPresent()) {
            Optional<Node> persistenceUnitProperties = persistenceUnitNode.getChildren()
                .stream()
                .filter(n -> n.getName().equals("properties"))
                .findFirst();
            if (persistenceUnitProperties.isPresent()) {
                persistenceUnitNode.removeChild("properties");
            }
            persistenceUnitNode.createChild("class")
                .text(entityQualifiedName);
            if (persistenceUnitProperties.isPresent()) {
                Node newPropertiesNode = persistenceUnitNode.createChild("properties");
                persistenceUnitProperties.get().getChildren()
                    .forEach(c -> newPropertiesNode.createChild("property")
                         .attribute("name", c.getAttribute("name"))
                         .attribute("value", c.getAttribute("value")));

            }
        }
        persistenceXMLFile.setContents(XMLParser.toXMLInputStream(node));
    }
}
