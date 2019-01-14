package com.github.adminfaces.addon.scaffold;

import static com.github.adminfaces.addon.util.Constants.NEW_LINE;
import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_PERSISTENCE_COORDINATE;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Plugin;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.javaee.faces.FacesFacet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.jpa.PersistenceMetaModelFacet;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.javaee.servlet.ServletFacet;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
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
import org.jboss.forge.furnace.services.Imported;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
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
import com.github.adminfaces.addon.freemarker.util.HasAutocompleteTagMethod;
import com.github.adminfaces.addon.freemarker.util.HasRelationShipMethod;
import com.github.adminfaces.addon.freemarker.util.HasSkipTagMethod;
import com.github.adminfaces.addon.scaffold.metamodel.AdminFacesMetaModelProvider;
import com.github.adminfaces.addon.ui.AdminFacesSetupCommand;
import com.github.adminfaces.addon.util.Constants;
import com.github.adminfaces.addon.util.DependencyUtil;

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

	@Inject
	private Imported<PersistenceMetaModelFacet> metaModelFacets;

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
		addAdminPersistence(project);
		configJPAMetaModel(project);
		return Collections.emptyList();
	}

	private void configJPAMetaModel(Project project) {
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
			Iterable<PersistenceMetaModelFacet> facets = facetFactory.createFacets(project, PersistenceMetaModelFacet.class);
			for (PersistenceMetaModelFacet metaModelFacet : facets) {
				metaModelFacet.setMetaModelProvider(new AdminFacesMetaModelProvider());
				if (facetFactory.install(project, metaModelFacet)) {
					break;
				}
			}
		}

	}

	private void addAdminPersistence(Project project) {
		DependencyBuilder adminPersistenceDependency = DependencyBuilder.create()
				.setCoordinate(dependencyUtil.getLatestVersion(ADMIN_PERSISTENCE_COORDINATE));
		dependencyUtil.installDependency(project.getFacet(DependencyFacet.class), adminPersistenceDependency);
		configDeltaSpike(project);
	}

	private void configDeltaSpike(Project project) {
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
		CDIFacet cdi = project.getFacet(CDIFacet.class);
		FileResource<?> beansXml = cdi.getConfigFile();
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
		WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
		for (Resource<?> resource : entities) {
			if (resource instanceof JavaResource) {
				JavaResource javaResource = (JavaResource) resource;
				try {
					JavaClassSource entity = (JavaClassSource) javaResource.getJavaType();
					entity.addImport("com.github.adminfaces.persistence.model.PersistenceEntity");
					entity.addInterface("PersistenceEntity");
					createOrOverwrite(java.getJavaResource(entity), entity.toString());
					context.put("entity", entity);
					String ccEntity = StringUtils.decapitalize(entity.getName());
					context.put("entityPackage", entity.getPackage());
					context.put("ccEntity", ccEntity);
					context.put("fields", entity.getFields());
					context.put("hasRelationShip", new HasRelationShipMethod());
					context.put("hasSkipJavadocTag", new HasSkipTagMethod());
					context.put("hasAutoCompleteJavadocTag", new HasAutocompleteTagMethod());
					setPrimaryKeyMetaData(context, entity);
					generateRepository(context, java, generatedResources);
					generateService(context, java, generatedResources);
					generateListMBean(context, java, generatedResources);
					generateFormMBean(context, java, generatedResources);
					addLeftMenuEntry(project, entity, generatedResources);
					addToptMenuEntry(project, entity, generatedResources);
					generateListPage(context, web, generatedResources);
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
	 * @param context            generation context containing entity
	 * @param java               Forge Java facet used to access java files
	 * @param generatedResources generated resources on current scaffold execution
	 */
	private void generateFormMBean(Map<Object, Object> context, JavaSourceFacet java,
			List<Resource<?>> generatedResources) {
		JavaClassSource formMB = Roaster.parse(JavaClassSource.class,
				FreemarkerTemplateProcessor.processTemplate(context, templates.getFormMBTemplate()));
		formMB.setPackage(java.getBasePackage() + "." + Constants.Packages.BEAN);
		JavaResource javaResource = java.getJavaResource(formMB);
		if (!javaResource.exists()) {
			generatedResources.add(createOrOverwrite(javaResource, formMB.toUnformattedString()));
		}
	}

	/**
	 * Generates JSF bean for list and search target entity
	 *
	 * @param context            generation context containing entity
	 * @param java               Forge Java facet used to access java files
	 * @param generatedResources generated resources on current scaffold execution
	 */
	private void generateListMBean(Map<Object, Object> context, JavaSourceFacet java,
			List<Resource<?>> generatedResources) {
		JavaClassSource listMB = Roaster.parse(JavaClassSource.class,
				FreemarkerTemplateProcessor.processTemplate(context, templates.getListMBTemplate()));
		listMB.setPackage(java.getBasePackage() + "." + Constants.Packages.BEAN);
		JavaResource javaResource = java.getJavaResource(listMB);
		if (!javaResource.exists()) {
			generatedResources.add(createOrOverwrite(javaResource, listMB.toUnformattedString()));
		}
	}

	/**
	 * Generates JPA repository for target entity
	 *
	 * @param context            generation context containing entity
	 * @param java               Forge Java facet used to access java files
	 * @param generatedResources generated resources on current scaffold execution
	 */
	private void generateRepository(Map<Object, Object> context, JavaSourceFacet java,
			List<Resource<?>> generatedResources) {
		JavaInterfaceSource repository = Roaster.parse(JavaInterfaceSource.class,
				FreemarkerTemplateProcessor.processTemplate(context, templates.getRepositoryTemplate()));
		repository.setPackage(java.getBasePackage() + "." + Constants.Packages.REPOSITORY);
		context.put("repository", repository);
		JavaResource javaResource = java.getJavaResource(repository);
		if (!javaResource.exists()) {
			generatedResources.add(createOrOverwrite(javaResource, repository.toUnformattedString()));
		}
	}

	/**
	 * Generates service for target entity
	 *
	 * @param context            generation context containing entity
	 * @param java               Forge Java facet used to access java files
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
		if (!javaResource.exists()) {
			generatedResources.add(createOrOverwrite(javaResource, service.toUnformattedString()));
		}
	}

	/**
	 * Generates entity list page
	 * 
	 * @param context            generation context containing entity
	 * @param web                Forge web facet used to access web resources
	 * @param generatedResources
	 */
	private void generateListPage(Map<Object, Object> context, WebResourcesFacet web,
			List<Resource<?>> generatedResources) {
		String listPage = FreemarkerTemplateProcessor.processTemplate(context, templates.getListPageTemplate());
		JavaClassSource entity = (JavaClassSource) context.get("entity");
		String entityName = entity.getName().toLowerCase();
		FileResource<?> listPageFile = web.getWebResource("/" + entityName + "/" + entityName + "-list.xhtml");
		if (!listPageFile.exists()) {
			generatedResources.add(createOrOverwrite(listPageFile,
					parser.parseInput(listPage, "UTF-8").outputSettings(outputSettings).toString()));
		}
	}

	void addLeftMenuEntry(Project project, JavaClassSource entity, List<Resource<?>> generatedResources) {
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
					+ "                        <i class=\"fa fa-circle-o\"></i>" + NEW_LINE
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

	void addToptMenuEntry(Project project, JavaClassSource entity, List<Resource<?>> generatedResources) {
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
					+ "                <i class=\"fa fa-circle-o\"></i>" + NEW_LINE + "            </a>" + NEW_LINE + ""
					+ NEW_LINE + "" + NEW_LINE + "            <ul class=\"dropdown-menu\" role=\"menu\">" + NEW_LINE
					+ "                <li>" + NEW_LINE + "                    <p:link outcome=\"" + listPage + "\">"
					+ NEW_LINE + "                        <span>List</span>" + NEW_LINE
					+ "                        <i class=\"fa fa-th-list\"></i>" + NEW_LINE
					+ "                    </p:link>" + NEW_LINE + "                </li>" + NEW_LINE
					+ "                <li>" + NEW_LINE + "                    <p:link outcome=\"" + formPage + "\">"
					+ NEW_LINE + "                        <span>New</span>" + NEW_LINE
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

	protected HashMap<Object, Object> getTemplateContext(String targetDir, final Resource<?> template) {
		HashMap<Object, Object> context = new HashMap<>();
		context.put("template", template);
		context.put("templatePath", Constants.WebResources.PAGE_TEMPLATE);
		context.put("targetDir", targetDir);
		return context;
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

}
