package com.github.adminfaces.addon.scaffold;

import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_PERSISTENCE_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_TEMPLATE_COORDINATE;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.javaee.ejb.EJBFacet;
import org.jboss.forge.addon.javaee.faces.FacesFacet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.javaee.servlet.ServletFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.scaffold.spi.AccessStrategy;
import org.jboss.forge.addon.scaffold.spi.ScaffoldGenerationContext;
import org.jboss.forge.addon.scaffold.spi.ScaffoldProvider;
import org.jboss.forge.addon.scaffold.spi.ScaffoldSetupContext;
import org.jboss.forge.addon.scaffold.ui.ScaffoldSetupWizard;
import org.jboss.forge.addon.ui.command.UICommand;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;

import com.github.adminfaces.addon.freemarker.TemplateFactory;
import com.github.adminfaces.addon.ui.AdminSetupCommand;
import com.github.adminfaces.addon.util.DependencyUtil;

public class AdminFacesScaffoldProvider implements ScaffoldProvider {

    private static final String INDEX_PAGE = "/index.xhtml";
    private static final String TEMPLATES = "/WEB-INF/templates";
    private static final String TEMPLATE_DEFAULT = TEMPLATES + "/template.xhtml";
    private static final String TEMPLATE_TOP = TEMPLATES + "/template-top.xhtml";
    private static final String INDEX_HTML = "/index.html";
    private static final String SCAFFOLD_PAGE_TEMPLATE = "#{layoutMB.template}";

    private static final Logger LOG = Logger.getLogger(AdminSetupCommand.class.getName());

    @Inject
    private TemplateFactory templates;

    @Inject
    private DependencyUtil dependencyUtil;

    private Project project;

    @Override
    public String getName() {
        return "AdminFaces";
    }

    @Override
    public String getDescription() {
        return "Enables Scaffold for AdminFaces projects using JPA entities";
    }

    @Override
    public List<Resource<?>> setup(ScaffoldSetupContext setupContext) {
        this.project = setupContext.getProject();
        addAdminPersistence();
        return Collections.emptyList();
    }

    private void addAdminPersistence() {
        DependencyBuilder adminPersistenceDependency = DependencyBuilder.create()
                .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_PERSISTENCE_COORDINATE));

        dependencyUtil.installDependency(project.getFacet(DependencyFacet.class), adminPersistenceDependency);

        configDeltaSpike(project);

    }

    private void configDeltaSpike(Project project) {
        Resource<?> resources = project.getFacet(ResourcesFacet.class).getResourceDirectory();

        if (!resources.getChild("apache-deltaspike.properties").exists()) {
            try(InputStream is = Thread.currentThread().getContextClassLoader()
                        .getResourceAsStream("/apache-deltaspike.properties")) {
                IOUtils.copy(is,
                        new FileOutputStream(
                                new File(resources.getFullyQualifiedName() + "/apache-deltaspike.properties")));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not add 'apache-deltaspike.properties'.", e);
            }
        }
        CDIFacet cdi = project.getFacet(CDIFacet.class);
        FileResource<?> beansXml = cdi.getConfigFile();
        Node node = XMLParser.parse(beansXml.getResourceInputStream());
        Node alternativesNode = node.getOrCreate("alternatives");
        Optional<Node> deltaspikeTransactionStrategy = alternativesNode.getChildren().stream()
                .filter(f -> f.getName().equals("class") && f.getText().contains("BeanManagedUserTransactionStrategy")).findFirst();

        if (!deltaspikeTransactionStrategy.isPresent()) {
            alternativesNode.createChild("class")
                    .text("org.apache.deltaspike.jpa.impl.transaction.BeanManagedUserTransactionStrategy");
            beansXml.setContents(XMLParser.toXMLInputStream(node));
        }

    }

    @Override
    public boolean isSetup(ScaffoldSetupContext setupContext) {
        Project project = setupContext.getProject();
        DependencyFacet facet = project.getFacet(DependencyFacet.class);
        boolean hasAdminFacesDependencies = facet
                .hasDirectDependency(DependencyBuilder.create().setArtifactId(ADMIN_TEMPLATE_COORDINATE.getArtifactId())
                        .setGroupId(ADMIN_TEMPLATE_COORDINATE.getGroupId()));

        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);

        boolean areResourcesInstalled = web.getWebResource(INDEX_PAGE).exists()
                && web.getWebResource(TEMPLATE_DEFAULT).exists()
                && web.getWebResource(TEMPLATE_TOP).exists();


        Resource<?> resources = project.getFacet(ResourcesFacet.class).getResourceDirectory();

        boolean hasAdminConfig = resources.getChild("admin-config.properties").exists();

        List<Class<? extends ProjectFacet>> requiredFacetsLists = Arrays.asList(WebResourcesFacet.class, DependencyFacet.class, JPAFacet.class,
                CDIFacet.class, ServletFacet.class, FacesFacet.class);

        boolean areRequiredFacetsInstalled = project.hasAllFacets(requiredFacetsLists);
        if(!areRequiredFacetsInstalled) {
            LOG.warning("AdminFaces scaffold provided not enabled because required facets (CDI, JPA, JSF and Servlet) are not installed. Use AdminFaces setup command to install required facets.");
        }

        return hasAdminFacesDependencies && areResourcesInstalled && hasAdminConfig && areRequiredFacetsInstalled;
    }

    @Override
    public List<Resource<?>> generateFrom(ScaffoldGenerationContext scaffoldGenerationContext) {
        System.out.println(scaffoldGenerationContext);
        return null;
    }

    @Override
    public NavigationResult getSetupFlow(ScaffoldSetupContext setupContext) {
        this.project = setupContext.getProject();
        NavigationResultBuilder builder = NavigationResultBuilder.create();
        List<Class<? extends UICommand>> setupCommands = new ArrayList<>();
        if (!project.hasFacet(JPAFacet.class)) {
            builder.add(JPASetupWizard.class);
        }
        Metadata compositeSetupMetadata = Metadata.forCommand(ScaffoldSetupWizard.class).name("Setup AdminFacets")
                .description("Setup all dependent facets for the AdminFaces scaffold.");
        builder.add(compositeSetupMetadata, setupCommands);
        return builder.build();
    }

    @Override
    public NavigationResult getGenerationFlow(ScaffoldGenerationContext scaffoldGenerationContext) {
        return null;
    }

    @Override
    public AccessStrategy getAccessStrategy() {
        return null;
    }

    protected HashMap<Object, Object> getTemplateContext(String targetDir, final Resource<?> template) {
        TemplateStrategy templateStrategy = getTemplateStrategy();

        HashMap<Object, Object> context;
        context = new HashMap<>();
        context.put("template", template);
        context.put("templatePath", SCAFFOLD_PAGE_TEMPLATE);
        context.put("templateStrategy", templateStrategy);
        context.put("targetDir", targetDir);
        return context;
    }

    public TemplateStrategy getTemplateStrategy() {
        return new AdminTemplateStrategy(this.project);
    }

}
