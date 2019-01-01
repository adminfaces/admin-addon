package com.github.adminfaces.addon.scaffold;

import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_PERSISTENCE_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_TEMPLATE_COORDINATE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.persistence.Id;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.cdi.CDIFacet;
import org.jboss.forge.addon.javaee.faces.FacesFacet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.jpa.ui.setup.JPASetupWizard;
import org.jboss.forge.addon.javaee.servlet.ServletFacet;
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
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.roaster.model.Field;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MemberSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.util.Types;
import org.metawidget.util.CollectionUtils;
import org.metawidget.util.simple.StringUtils;

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

		boolean areResourcesInstalled = web.getWebResource(INDEX_PAGE).exists()
				&& web.getWebResource(TEMPLATE_DEFAULT).exists() && web.getWebResource(TEMPLATE_TOP).exists();

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
		Collection<Resource<?>> entities = scaffoldGenerationContext.getResources();
		List<Resource<?>> generatedResources = new ArrayList<>();
		Map<Object, Object> context = CollectionUtils.newHashMap();
		for (Resource<?> resource : entities) {
			if (resource instanceof JavaResource) {
				JavaResource javaResource = (JavaResource) resource;
				try {
					JavaClassSource entity = (JavaClassSource) javaResource.getJavaType();
					context.put("entity", entity);
					String ccEntity = StringUtils.decapitalize(entity.getName());
					context.put("ccEntity", ccEntity);
					setPrimaryKeyMetaData(context, entity);
					Resource<?> generatedService = generateService(entity);
					// TODO add service to freemarker context
					generatedResources.add(generateListMBean(entity));
					generatedResources.add(generateFormMBean(entity));
				} catch (FileNotFoundException fileEx) {
					throw new IllegalStateException(fileEx);
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
	 * @param entitySource target entity
	 * @return fully qualified name of generated Bean
	 */
	private Resource<?> generateFormMBean(JavaSource<?> entitySource) {

		throw new NotImplementedException("TODO");
	}

	/**
	 * Generates JSF bean for list and search target entity
	 * 
	 * @param entitySource target entity
	 * @return fully qualified name of generated Bean
	 */
	private Resource<?> generateListMBean(JavaSource<?> entitySource) {
		throw new NotImplementedException("TODO");
	}

	/**
	 * Generates service for target entity
	 * 
	 * @param entitySource target entity
	 * @return fully qualified name of generated Service
	 */
	private Resource<?> generateService(JavaSource<?> entitySource) {
		throw new NotImplementedException("TODO");
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

	/**
	 * Copied from forge-core/javaee/scafold-faces/FacesScaffoldProvider.java
	 * @param context
	 * @param entity
	 */
	private void setPrimaryKeyMetaData(Map<Object, Object> context, final JavaClassSource entity) {
		String pkName = "id";
		String pkType = "Long";
		String nullablePkType = "Long";
		for (MemberSource<JavaClassSource, ?> m : entity.getMembers()) {
			if (m.hasAnnotation(Id.class)) {
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
