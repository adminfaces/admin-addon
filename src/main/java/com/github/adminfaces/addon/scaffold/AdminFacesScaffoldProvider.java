package com.github.adminfaces.addon.scaffold;

import static com.github.adminfaces.addon.util.Constants.NEW_LINE;
import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_TEMPLATE_COORDINATE;
import static org.jboss.forge.addon.scaffold.util.ScaffoldUtil.createOrOverwrite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.javaee.faces.FacesFacet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.javaee.servlet.ServletFacet;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
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
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.util.Types;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Parser;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.simple.StringUtils;

import com.github.adminfaces.addon.freemarker.FreemarkerTemplateProcessor;
import com.github.adminfaces.addon.freemarker.TemplateFactory;
import com.github.adminfaces.addon.scaffold.model.EntityConfig;
import com.github.adminfaces.addon.scaffold.config.ScaffoldConfigLoader;
import com.github.adminfaces.addon.scaffold.model.ScaffoldEntity;
import com.github.adminfaces.addon.ui.AdminFacesSetupCommand;
import com.github.adminfaces.addon.util.AdminScaffoldUtils;
import com.github.adminfaces.addon.util.Constants;
import com.github.adminfaces.addon.util.DependencyUtil;
import org.jboss.forge.roaster.model.source.FieldSource;

public class AdminFacesScaffoldProvider implements ScaffoldProvider {

    private static final Logger LOG = Logger.getLogger(AdminFacesSetupCommand.class.getName());

    private final Document.OutputSettings outputSettings = new Document.OutputSettings().prettyPrint(true)
        .charset("UTF-8").indentAmount(4).syntax(Document.OutputSettings.Syntax.xml);

    private final Parser parser = Parser.xmlParser().settings(new ParseSettings(true, true));

    @Inject
    private TemplateFactory templates;

    @Inject
    private DependencyUtil dependencyUtil;

    @Inject
    private FacetFactory facetFactory;

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
        Project project = setupContext.getProject();
        AdminScaffoldUtils.setupAdminPersistece(project,dependencyUtil, facetFactory);
        createScaffoldConfig(project);
        addAppListCache(project);
        return Collections.emptyList();
    }

    /**
     * Just adds global scaffold config file to the project
     *
     * @param project
     */
    private void createScaffoldConfig(Project project) {
        DirectoryResource resources = project.getFacet(ResourcesFacet.class).getResourceDirectory();
        DirectoryResource scaffoldDir = resources.getOrCreateChildDirectory("scaffold");
        if (!scaffoldDir.getChild("global-config.yml").exists()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/scaffold/global-config.yml")) {
                IOUtils.copy(is,
                    new FileOutputStream(new File(scaffoldDir.getFullyQualifiedName() + "/global-config.yml")));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not create 'global-config.yml'.", e);
            }
        }

        MavenFacet m2 = project.getFacet(MavenFacet.class);
        MavenModelResource m2Model = m2.getModelResource();

        Node node = XMLParser.parse(m2Model.getResourceInputStream());
        Node resourcesNode = node.getOrCreate("build").getOrCreate("resources");
        Optional<Node> resourcesDirectory = resourcesNode.get("resource").stream()
            .filter(r -> r.getName().equals("directory") && r.getText().equals("src/main/resources")).findFirst();

        Node resourceNode = resourcesNode.getOrCreate("resource");
        if (!resourcesDirectory.isPresent()) {
            resourceNode.getOrCreate("filtering").text("true");
            resourcesDirectory = Optional.of(resourceNode.getOrCreate("directory"));
            resourcesDirectory.get().text("src/main/resources");
        }
        Node resourcesExclusions = resourceNode.getOrCreate("excludes");

        Optional<Node> scaffoldExclude = resourcesExclusions.getChildren().stream()
            .filter(e -> e.getName().equals("exclude") && e.getText().contains("scaffold"))
            .findFirst();

        if (!scaffoldExclude.isPresent()) {
            Node resource = resourcesExclusions.createChild("exclude");
            resource.text("scaffold/**");
            m2Model.setContents(XMLParser.toXMLInputStream(node));
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

        boolean areResourcesInstalled = web.getWebResource(Constants.WebResources.INDEX_PAGE).exists()
            && web.getWebResource(Constants.WebResources.TEMPLATE_DEFAULT).exists()
            && web.getWebResource(Constants.WebResources.TEMPLATE_TOP).exists();

        Resource<?> resources = project.getFacet(ResourcesFacet.class).getResourceDirectory();

        boolean hasAdminConfig = resources.getChild("admin-config.properties").exists();

        List<Class<? extends ProjectFacet>> requiredFacetsLists = Arrays.asList(WebResourcesFacet.class,
            DependencyFacet.class, JPAFacet.class, CDIFacet.class, ServletFacet.class, FacesFacet.class);

        boolean areRequiredFacetsInstalled = project.hasAllFacets(requiredFacetsLists);
        if (!areRequiredFacetsInstalled) {
            LOG.warning("AdminFaces scaffold provided not enabled because required facets "
                + "(CDI, JPA, JSF and Servlet) are not installed. Use AdminFaces setup command to install required facets.");
        }

        return hasAdminFacesDependencies && areResourcesInstalled && hasAdminConfig && areRequiredFacetsInstalled;
    }

    @Override
    public List<Resource<?>> generateFrom(ScaffoldGenerationContext scaffoldGenerationContext) {
        Project project = scaffoldGenerationContext.getProject();
        Collection<Resource<?>> entities = scaffoldGenerationContext.getResources();
        List<Resource<?>> generatedResources = new ArrayList<>();
        Map<Object, Object> context = CollectionUtils.newHashMap();
        JavaSourceFacet java = project.getFacet(JavaSourceFacet.class);
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        String appListsPackage = metadataFacet.getProjectGroupName() + "." + Constants.Packages.BEAN + ".AppLists";
        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        for (Resource<?> resource : entities) {
            context.put("appListsPackage", appListsPackage);
            if (resource instanceof JavaResource) {
                JavaResource javaResource = (JavaResource) resource;
                try {
                    JavaClassSource entity = javaResource.getJavaType();
                    entity.addImport("com.github.adminfaces.persistence.model.PersistenceEntity");
                    entity.addInterface("PersistenceEntity");
                    createOrOverwrite(java.getJavaResource(entity), entity.toString());
                    EntityConfig entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
                    ScaffoldEntity scaffoldEntity = new ScaffoldEntity(entity, entityConfig, project);
                    context.put("entity", scaffoldEntity);
                    String ccEntity = StringUtils.decapitalize(entity.getName());
                    context.put("entityPackage", entity.getPackage());
                    context.put("ccEntity", ccEntity);
                    context.put("fields", scaffoldEntity.getFields());
                    context.put("embeddedFields", scaffoldEntity.getEmbeddedFields());
                    context.put("toManyFields", resolveToManyAssociationFields(scaffoldEntity.getFields()));
                    context.put("toOneFields", resolveToOneAssociationFields(scaffoldEntity.getFields()));
                    setPrimaryKeyMetaData(context, entity);
                    generateRepository(context, java, generatedResources);
                    generateService(context, java, generatedResources);
                    generateListMBean(context, java, generatedResources);
                    generateFormMBean(context, java, generatedResources);
                    updateAppListsCache(entity, project, context);
                    addLeftMenuEntry(project, scaffoldEntity, generatedResources);
                    addToptMenuEntry(project, scaffoldEntity, generatedResources);
                    generateListPage(context, web, generatedResources);
                    generateFormPage(context, web, generatedResources);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Problems during AdminFaces scaffold execution.", e);
                    throw new RuntimeException(e);
                } finally {
                    context.clear();
                }
            }
        }
        return generatedResources;
    }

    /**
     * Generates JSF bean for Create and update target entity
     *
     * @param context generation context containing entity
     * @param java Forge Java facet used to access java files
     * @param generatedResources generated resources on current scaffold execution
     */
    private void generateFormMBean(Map<Object, Object> context, JavaSourceFacet java,
        List<Resource<?>> generatedResources) {
        JavaClassSource formMB = Roaster.parse(JavaClassSource.class,
            FreemarkerTemplateProcessor.processTemplate(context, templates.getFormMBTemplate()));
        formMB.setPackage(java.getBasePackage() + "." + Constants.Packages.BEAN);
        JavaResource javaResource = java.getJavaResource(formMB);
        generatedResources.add(createOrOverwrite(javaResource, formMB.toUnformattedString()));
    }

    /**
     * Generates JSF bean for list and search target entity
     *
     * @param context generation context containing entity
     * @param java Forge Java facet used to access java files
     * @param generatedResources generated resources on current scaffold execution
     */
    private void generateListMBean(Map<Object, Object> context, JavaSourceFacet java,
        List<Resource<?>> generatedResources) {
        JavaClassSource listMB = Roaster.parse(JavaClassSource.class,
            FreemarkerTemplateProcessor.processTemplate(context, templates.getListMBTemplate()));
        listMB.setPackage(java.getBasePackage() + "." + Constants.Packages.BEAN);
        JavaResource javaResource = java.getJavaResource(listMB);
        generatedResources.add(createOrOverwrite(javaResource, listMB.toUnformattedString()));
    }

    /**
     * Generates JPA repository for target entity
     *
     * @param context generation context containing entity
     * @param java Forge Java facet used to access java files
     * @param generatedResources generated resources on current scaffold execution
     */
    private void generateRepository(Map<Object, Object> context, JavaSourceFacet java,
        List<Resource<?>> generatedResources) {
        JavaInterfaceSource repository = Roaster.parse(JavaInterfaceSource.class,
            FreemarkerTemplateProcessor.processTemplate(context, templates.getRepositoryTemplate()));
        repository.setPackage(java.getBasePackage() + "." + Constants.Packages.REPOSITORY);
        context.put("repository", repository);
        JavaResource javaResource = java.getJavaResource(repository);
        generatedResources.add(createOrOverwrite(javaResource, repository.toUnformattedString()));
    }

    /**
     * Generates service for target entity
     *
     * @param context generation context containing entity
     * @param java Forge Java facet used to access java files
     * @param generatedResources generated resources on current scaffold execution
     *
     */
    private void generateService(Map<Object, Object> context, JavaSourceFacet java,
        List<Resource<?>> generatedResources) {
        JavaClassSource service = Roaster.parse(JavaClassSource.class,
            FreemarkerTemplateProcessor.processTemplate(context, templates.getServiceTemplate()));
        service.setPackage(java.getBasePackage() + "." + Constants.Packages.SERVICE);
        context.put("service", service);
        JavaResource javaResource = java.getJavaResource(service);
        generatedResources.add(createOrOverwrite(javaResource, service.toUnformattedString()));
    }

    /**
     * Generates entity list page
     *
     * @param context generation context containing entity
     * @param web Forge web facet used to access web resources
     * @param generatedResources
     */
    private void generateListPage(Map<Object, Object> context, WebResourcesFacet web,
        List<Resource<?>> generatedResources) {
        String listPage = FreemarkerTemplateProcessor.processTemplate(context, templates.getListPageTemplate());
        ScaffoldEntity entity = (ScaffoldEntity) context.get("entity");
        String entityName = entity.getName().toLowerCase();
        FileResource<?> listPageFile = web.getWebResource("/" + entityName + "/" + entityName + "-list.xhtml");
        generatedResources.add(createOrOverwrite(listPageFile,
            parser.parseInput(listPage, "UTF-8").outputSettings(outputSettings).toString()));
    }

    /**
     * Generates entity form page
     *
     * @param context generation context containing entity
     * @param web Forge web facet used to access web resources
     * @param generatedResources
     */
    private void generateFormPage(Map<Object, Object> context, WebResourcesFacet web,
        List<Resource<?>> generatedResources) {
        String listPage = FreemarkerTemplateProcessor.processTemplate(context, templates.getFormPageTemplate());
        ScaffoldEntity entity = (ScaffoldEntity) context.get("entity");
        String entityName = entity.getName().toLowerCase();
        FileResource<?> formPageFile = web.getWebResource("/" + entityName + "/" + entityName + "-form.xhtml");
        generatedResources.add(createOrOverwrite(formPageFile,
            parser.parseInput(listPage, "UTF-8").outputSettings(outputSettings).toString()));
    }

    void addLeftMenuEntry(Project project, ScaffoldEntity entity, List<Resource<?>> generatedResources) {
        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        FileResource<?> leftMenu = web.getWebResource(Constants.WebResources.LEFT_MENU);
        Document leftMenuDocument = Jsoup.parse(leftMenu.getContents(Charset.forName("UTF-8")));
        Element menuContent = leftMenuDocument.getElementsByClass("sidebar-menu").get(0);

        String entityName = entity.getName();
        boolean menuEntryExists = menuContent.getElementById("menu" + entityName) != null;
        if (!menuEntryExists) {
            String pageFolder = "/" + entityName.toLowerCase() + "/";
            String listPage = pageFolder + entityName.toLowerCase() + "-list.xhtml";
            menuContent.append("<li>" + NEW_LINE + "                    <p:link id=\"menu" + entityName
                + "\" outcome=\"" + listPage + "\" title=\"" + entityName + "s page\">" + NEW_LINE
                + "                        <i class=\"" + entity.getEntityConfig().getMenuIcon() + "\"></i>" + NEW_LINE
                + "                        <span>" + entityName + "s</span>" + NEW_LINE
                + "                    </p:link>" + NEW_LINE + "                </li>");

            String content = leftMenuDocument.body().toString();
            int startIndex = content.indexOf("<ui:composition");
            int endIndex = content.indexOf("</body");
            leftMenu.setContents(parser
                .parseInput("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + content.substring(startIndex, endIndex),
                    "UTF-8")
                .outputSettings(outputSettings).toString());
            generatedResources.add(leftMenu);
        }
    }

    void addToptMenuEntry(Project project, ScaffoldEntity entity, List<Resource<?>> generatedResources) {
        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        FileResource<?> topMenu = web.getWebResource(Constants.WebResources.TOP_MENU);
        Document leftMenuDocument = Jsoup.parse(topMenu.getContents(Charset.forName("UTF-8")));
        Element menuContent = leftMenuDocument.getElementsByClass("navbar-nav").get(0);

        String entityName = entity.getName();
        boolean menuEntryExists = menuContent.getElementById("menu" + entityName) != null;
        if (!menuEntryExists) {
            String pageFolder = "/" + entityName.toLowerCase() + "/";
            String listPage = pageFolder + entityName.toLowerCase() + "-list.xhtml";
            String formPage = pageFolder + entityName.toLowerCase() + "-form.xhtml";
            menuContent.append("<li id=\"" + "menu" + entityName + "\" class=\"dropdown\">" + NEW_LINE + NEW_LINE
                + "            <a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">" + entityName
                + "s <span" + NEW_LINE + "                    class=\"caret\"></span>" + NEW_LINE
                + "                <i class=\"" + entity.getEntityConfig().getMenuIcon() + "\"></i>" + NEW_LINE + "            </a>" + NEW_LINE + ""
                + NEW_LINE + "" + NEW_LINE + "            <ul class=\"dropdown-menu\" role=\"menu\">" + NEW_LINE
                + "                <li>" + NEW_LINE + "                    <p:link outcome=\"" + listPage + "\">"
                + NEW_LINE + "                        <span>List " + entityName.toLowerCase() + "s</span>" + NEW_LINE
                + "                        <i class=\"fa fa-th-list\"></i>" + NEW_LINE
                + "                    </p:link>" + NEW_LINE + "                </li>" + NEW_LINE
                + "                <li>" + NEW_LINE + "                    <p:link outcome=\"" + formPage + "\">"
                + NEW_LINE + "                        <span>New " + entityName.toLowerCase() + "</span>" + NEW_LINE
                + "                        <i class=\"fa fa-plus-circle\"></i>" + NEW_LINE
                + "                    </p:link>" + NEW_LINE + "                </li>" + NEW_LINE + "" + NEW_LINE
                + "" + NEW_LINE + "            </ul>" + NEW_LINE + "" + NEW_LINE + "        </li>");

            String content = leftMenuDocument.body().toString();
            int startIndex = content.indexOf("<ui:composition");
            int endIndex = content.indexOf("</body");
            topMenu.setContents(parser
                .parseInput("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + content.substring(startIndex, endIndex),
                    "UTF-8")
                .outputSettings(outputSettings).toString());
            generatedResources.add(topMenu);
        }
    }

    @Override
    public NavigationResult getSetupFlow(ScaffoldSetupContext setupContext) {
        Project project = setupContext.getProject();
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
    public NavigationResult getGenerationFlow(ScaffoldGenerationContext generationContext) {
        NavigationResultBuilder builder = NavigationResultBuilder.create();
        builder.add(AdminFacesEntitySelectionWizard.class);
        return builder.build();
    }

    @Override
    public AccessStrategy getAccessStrategy() {
        return null;
    }

    /**
     * Copied from forge-core/javaee/scafold-faces/FacesScaffoldProvider.java
     *
     * @param context
     * @param entity
     */
    private void setPrimaryKeyMetaData(Map<Object, Object> context, final JavaClassSource entity) {
        String pkName = "id";
        String pkType = "Long";
        String nullablePkType = "Long";
        for (MemberSource<JavaClassSource, ?> m : entity.getMembers()) {
            if (m.hasAnnotation(Id.class) || m.hasAnnotation(EmbeddedId.class)) {
                if (m instanceof Field) {
                    Field<?> field = (Field<?>) m;
                    pkName = field.getName();
                    pkType = field.getType().getQualifiedName();
                    nullablePkType = pkType;
                    break;
                }

                MethodSource<?> method = (MethodSource<?>) m;
                pkName = method.getName().substring(3);
                if (method.getName().startsWith("get")) {
                    pkType = method.getReturnType().getQualifiedName();
                } else {
                    pkType = method.getParameters().get(0).getType().getQualifiedName();
                }
                nullablePkType = pkType;
                break;
            }
        }

        if (Types.isJavaLang(pkType)) {
            nullablePkType = Types.toSimpleName(pkType);
        } else if ("int".equals(pkType)) {
            nullablePkType = Integer.class.getSimpleName();
        } else if ("short".equals(pkType)) {
            nullablePkType = Short.class.getSimpleName();
        } else if ("byte".equals(pkType)) {
            nullablePkType = Byte.class.getSimpleName();
        } else if ("long".equals(pkType)) {
            nullablePkType = Long.class.getSimpleName();
        }

        context.put("primaryKey", pkName);
        context.put("primaryKeyCC", StringUtils.capitalize(pkName));
        context.put("primaryKeyType", pkType);
        context.put("nullablePrimaryKeyType", nullablePkType);
    }

    private List<FieldSource<JavaClassSource>> resolveToManyAssociationFields(List<FieldSource<JavaClassSource>> fields) {
        List<FieldSource<JavaClassSource>> toManyFields = new ArrayList<>();
        for (FieldSource<JavaClassSource> field : fields) {
            if (AdminScaffoldUtils.hasToManyAssociation(field)) {
                toManyFields.add(field);
            }
        }
        return toManyFields;
    }

    private List<FieldSource<JavaClassSource>> resolveToOneAssociationFields(List<FieldSource<JavaClassSource>> fields) {
        List<FieldSource<JavaClassSource>> toOneFields = new ArrayList<>();
        for (FieldSource<JavaClassSource> field : fields) {
            if (AdminScaffoldUtils.hasToOneAssociation(field)) {
                toOneFields.add(field);
            }
        }
        return toOneFields;
    }

    private void addAppListCache(Project project) {
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);
        boolean appListExists = new File((AdminScaffoldUtils.resolveSourceFolder(project) + "/" +
            metadataFacet.getProjectGroupName() + "/"+Constants.Packages.BEAN+"/").replaceAll("\\.", "/")+"AppLists.java").exists();
        
        if(!appListExists) {
            try (InputStream appListsStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/bean/AppLists.java")) {
                JavaSource<?> appLists = (JavaSource<?>) Roaster.parse(appListsStream);
                appLists.setPackage(metadataFacet.getProjectGroupName() + "."+Constants.Packages.BEAN);
                javaSource.saveJavaSource(appLists);
                FileUtils.copyInputStreamToFile(appListsStream, new File(project.getRoot().getFullyQualifiedName()
                    + "/"+appLists.getPackage().replaceAll("\\.", "/")));
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Could not add 'AppLists'.", e);
            }
        }
    }

    private void updateAppListsCache(JavaClassSource entity, Project project, Map<Object, Object> context) {
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        JavaResource appListsFile = sourceFacet.getJavaResource((metadataFacet.getProjectGroupName() + "/" + Constants.Packages.BEAN + "/AppLists").replaceAll("\\.", "/")+".java");
        JavaClassSource appListsSource = Roaster.parse(JavaClassSource.class, appListsFile.getResourceInputStream());
        String ccEntity = (String) context.get("ccEntity");
        String instanceVariableName = ccEntity + "s;";//e.g Room becomes rooms;
        String serviceName = ccEntity+"Service";
        if(!appListsSource.hasField(serviceName)) {
            FieldSource<JavaClassSource> serviceField = appListsSource.addField("private CrudService<"+entity.getName()+", "+context.get("nullablePrimaryKeyType")+ "> "+serviceName+";\n");
            serviceField.addAnnotation("javax.inject.Inject");
            serviceField.addAnnotation("com.github.adminfaces.persistence.service.Service");
        }
        if (!appListsSource.hasField(instanceVariableName)) {
            String field = "private Set<ENTITY> ".replace("ENTITY", entity.getName()).concat(instanceVariableName)+"\n";
            appListsSource.addField(field);
        }
        
        String entityImport = entity.getPackage() + "."+entity.getName();
        if (!appListsSource.hasImport(entityImport)) {
            appListsSource.addImport(entityImport);
        }
        String methodName = "all"+entity.getName()+"s";//Room becomes allRooms()
        if(!appListsSource.hasMethodSignature(methodName)) {
            MethodSource<JavaClassSource> method = appListsSource.addMethod()
                .setPublic()
                .setName(methodName)
                .setBody(("if(ENTITYs == null) {\n" +
"            ENTITYs = new HashSet<>(ENTITYService.criteria()\n" +
"           .getResultList());\n" +
"        }\n" +
"               return ENTITYs;").replaceAll("ENTITY", ccEntity))
                .setReturnType("java.util.Set<" + entity.getName() + ">");
            method.addAnnotation("javax.enterprise.inject.Produces");
            method.addAnnotation("javax.inject.Named")
                .setStringValue("value",methodName);
            
            methodName = "clear"+entity.getName()+"s";//Room becomes clearRooms()
            method = appListsSource.addMethod()
                .setPublic()
                .setName(methodName)
                .setReturnTypeVoid()
                .setBody(ccEntity+"s = null;");
            sourceFacet.saveJavaSourceUnformatted(appListsSource);
        }
    }

}
