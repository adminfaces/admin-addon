package com.github.adminfaces.addon.ui;

import static org.jboss.forge.addon.scaffold.util.ScaffoldUtil.createOrOverwrite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.javaee.faces.FacesFacet;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.shrinkwrap.descriptor.api.javaee.ParamValueCommonType;
import org.jboss.shrinkwrap.descriptor.api.javaee7.ParamValueType;
import org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor;
import org.metawidget.util.simple.StringUtils;

import com.github.adminfaces.addon.facet.AdminFacet;
import com.github.adminfaces.addon.freemarker.FreemarkerTemplateProcessor;
import com.github.adminfaces.addon.freemarker.TemplateFactory;

/**
 * AdminFaces: Setup command
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
@FacetConstraint({ JavaEE7Facet.class, WebResourcesFacet.class })
public class AdminSetupCommand extends AbstractProjectCommand {

	private static final String PAGE_TEMPLATE = "#{layoutMB.template}";

	private static final String INDEX_PAGE = "/index.xhtml";

	private static final String INCLUDES = "/includes";

	private static final String TEMPLATES = "/WEB-INF/templates";

	private static final String TEMPLATE_DEFAULT = TEMPLATES + "/template.xhtml";

	private static final String TEMPLATE_TOP = TEMPLATES + "/template-top.xhtml";

	private static final String INDEX_HTML = "/index.html";

	private static final String LOGIN_PAGE = "/login.xhtml";

	private static final String SCAFFOLD_RESOURCES = "/scaffold";

	private static final Logger LOG = Logger.getLogger(AdminSetupCommand.class.getName());

	@Inject
	private FacetFactory facetFactory;

	@Inject
	private ProjectFactory projectFactory;

	@Inject
	private TemplateFactory templates;

	@Override
	public UICommandMetadata getMetadata(UIContext context) {
		return Metadata.forCommand(getClass()).name("AdminFaces: Setup").category(Categories.create("AdminFaces"))
				.description("Setup AdminFaces dependencies in the current project.");
	}

	@Override
	protected ProjectFactory getProjectFactory() {
		return projectFactory;
	}

	@Override
	protected boolean isProjectRequired() {
		return true;
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {

		final Project project = getSelectedProject(context) != null ? getSelectedProject(context)
				: getSelectedProject(context.getUIContext());

		boolean execute = true;
		if (project.hasFacet(AdminFacet.class) && project.getFacet(AdminFacet.class).isInstalled()) {
			execute = context.getPrompt().promptBoolean("AdminFaces is already installed, override it?");
		}

		if (!execute) {
			return Results.success();
		}
		List<Result> results = new ArrayList<>();
		AdminFacet facet = facetFactory.create(project, AdminFacet.class);
		facetFactory.install(project, facet);
		results.add(Results.success("AdminFaces setup completed successfully!"));

		if (!project.hasFacet(ServletFacet_3_1.class)) {
			ServletFacet_3_1 servletFacet_3_1 = facetFactory.create(project, ServletFacet_3_1.class);
			facetFactory.install(project, servletFacet_3_1);
		}

		if (!project.hasFacet(FacesFacet_2_0.class)) {
			FacesFacet_2_0 facesFacet = facetFactory.create(project, FacesFacet_2_0.class);
			facetFactory.install(project, facesFacet);
		}

		MavenFacet m2 = project.getFacet(MavenFacet.class);
		MavenModelResource m2Model = m2.getModelResource();

		Node node = XMLParser.parse(m2Model.getResourceInputStream());
		Node resourcesNode = node.getOrCreate("build").getOrCreate("resources");
		Optional<Node> resourcesFiltering = resourcesNode.get("resource").stream()
				.filter(r -> r.getName().equals("directory") && r.getText().equals("src/main/resources")).findFirst();

		if (!resourcesFiltering.isPresent()) {
			Node resource = resourcesNode.createChild("resource");
			resource.createChild("filtering").text("true");
			resource.createChild("directory").text("src/main/resources");
			m2Model.setContents(XMLParser.toXMLInputStream(node));
		}

		addAdminFacesResources(project).forEach(r -> results.add(Results
				.success("Added " + r.getFullyQualifiedName().replace(project.getRoot().getFullyQualifiedName(), ""))));
		setupWebXML(project);

		return Results.aggregate(results);

	}

	private String resolveLogoMini(String projectName) {
		if (projectName.length() > 3) {
			return projectName.substring(0, 3);
		} else {
			return projectName;
		}
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
	}

	@SuppressWarnings("rawtypes")
	protected List<Resource<?>> addAdminFacesResources(Project project) {
		List<Resource<?>> result = new ArrayList<>();
		WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
		JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);

		AdminFacet adminFacet = project.getFacet(AdminFacet.class);
		ServletFacet_3_1 servlet = project.getFacet(ServletFacet_3_1.class);

		org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor servletConfig = (org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor) servlet
				.getConfig();
		servletConfig.getOrCreateWelcomeFileList().welcomeFile(INDEX_HTML);

		HashMap<Object, Object> context = getTemplateContext();
		MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
		String projectName = metadataFacet.getProjectName();
		String logoMini = resolveLogoMini(projectName);
		context.put("appName", StringUtils.uncamelCase(projectName));
		context.put("logoMini", logoMini);
		context.put("copyrightYear", Year.now().toString());

		// admin config
		addAdminConfig(project);
		// Basic pages

		result.add(createOrOverwrite(web.getWebResource(INDEX_PAGE),
				FreemarkerTemplateProcessor.processTemplate(context, templates.getIndexTemplate())));

		result.add(createOrOverwrite(web.getWebResource(LOGIN_PAGE),
				FreemarkerTemplateProcessor.processTemplate(context, templates.getLoginTemplate())));

		// templates

		result.add(createOrOverwrite(web.getWebResource(TEMPLATE_DEFAULT),
				FreemarkerTemplateProcessor.processTemplate(context, templates.getTemplateDefault())));

		result.add(createOrOverwrite(web.getWebResource(TEMPLATE_TOP),
				FreemarkerTemplateProcessor.processTemplate(context, templates.getTemplateTop())));

		// menus

		result.add(createOrOverwrite(web.getWebResource(INCLUDES + "/menu.xhtml"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + INCLUDES + "/menu.xhtml")));

		result.add(createOrOverwrite(web.getWebResource(INCLUDES + "/menubar.xhtml"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + INCLUDES + "/menubar.xhtml")));

		result.add(createOrOverwrite(web.getWebResource(INCLUDES + "/top-bar.xhtml"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + INCLUDES + "/top-bar.xhtml")));

		if (!web.getWebResource("WEB-INF/beans.xml").exists()) {
			result.add(createOrOverwrite(web.getWebResource("WEB-INF/beans.xml"),
					getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/WEB-INF/beans.xml")));
		}

		// beans
		try (InputStream logonStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("/infra/security/LogonMB.java")) {
			JavaSource<?> logonMB = (JavaSource<?>) Roaster.parse(logonStream);
			logonMB.setPackage(metadataFacet.getProjectGroupName() + ".infra");
			javaSource.saveJavaSource(logonMB);
			FileUtils.copyInputStreamToFile(logonStream,
					new File(project.getRoot().getFullyQualifiedName() + logonMB.getPackage().replaceAll("\\.", "/")));
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Could not add 'LogonMB'.", e);
		}

		// Static resources

		result.add(createOrOverwrite(web.getWebResource("/resources/favicon/favicon.ico"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon.ico")));

		result.add(createOrOverwrite(web.getWebResource("/resources/favicon/favicon-16x16.png"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon-16x16.png")));

		result.add(createOrOverwrite(web.getWebResource("/resources/favicon/favicon-32x32.png"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon-32x32.png")));

		result.add(createOrOverwrite(web.getWebResource("/resources/favicon/favicon-96x96.png"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/favicon-96x96.png")));

		result.add(createOrOverwrite(web.getWebResource("/resources/images/login-bg.jpg"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/images/login-bg.jpg")));

		result.add(createOrOverwrite(web.getWebResource("/resources/css/app.css"),
				getClass().getResourceAsStream(SCAFFOLD_RESOURCES + "/css/app.css")));

		return result;
	}

	private void addAdminConfig(Project project) {

		Resource<?> resources = project.getRoot().reify(DirectoryResource.class).getChildDirectory("src")
				.getChildDirectory("main").getOrCreateChildDirectory("resources");

		if (!resources.getChild("admin-config.properties").exists()) {
			try {
				IOUtils.copy(
						Thread.currentThread().getContextClassLoader().getResourceAsStream("/admin-config.properties"),
						new FileOutputStream(new File(resources.getFullyQualifiedName() + "/admin-config.properties")));
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Could not add 'admin-config.properties'.", e);
			}

		}

		if (!resources.getChild("messages.properties").exists()) {
			try {
				IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("/messages.properties"),
						new FileOutputStream(new File(resources.getFullyQualifiedName() + "/messages.properties")));
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Could not add 'admin-config.properties'.", e);
			}

		}

	}

	protected void setupWebXML(Project project) {
		ServletFacet_3_1 servlet = project.getFacet(ServletFacet_3_1.class);
		WebAppDescriptor servletConfig = (WebAppDescriptor) servlet.getConfig();

		// Use the server timezone since we accept dates in that timezone, and it makes
		// sense to display them in the
		// same
		boolean found = false;
		List<ParamValueType<WebAppDescriptor>> allContextParam = servletConfig.getAllContextParam();
		for (ParamValueCommonType<?> contextParam : allContextParam) {
			if (contextParam.getParamName()
					.equals("javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE")) {
				found = true;
			}
		}
		if (!found) {
			servletConfig.createContextParam()
					.paramName("javax.faces.DATETIMECONVERTER_DEFAULT_TIMEZONE_IS_SYSTEM_TIMEZONE").paramValue("true");
		}

		configPrimeFaces(servletConfig, allContextParam);

		FacesFacet facesFacet = project.getFacet(FacesFacet.class);

		configOmniFaces(servletConfig, facesFacet);

		setupErrorPages(facesFacet, servletConfig);

		servlet.saveConfig(servletConfig);
	}

	private void setupErrorPages(FacesFacet facesFacet, WebAppDescriptor servletConfig) {

		String pageSuffix = ".xhtml";
		List<String> pageSuffixes = facesFacet.getFacesSuffixes();
		if (pageSuffixes != null || !pageSuffixes.isEmpty()) {
			pageSuffix = pageSuffixes.get(0);
		}

		if (!pageSuffix.equals(".xhtml")) {
			servletConfig.createErrorPage().errorCode("401").location("/401" + pageSuffix);

			servletConfig.createErrorPage().errorCode("403").location("/403" + pageSuffix);

			servletConfig.createErrorPage()
					.exceptionType("com.github.adminfaces.template.exception.AccessDeniedException")
					.location("/403" + pageSuffix);

			servletConfig.createErrorPage().errorCode("404").location("/404" + pageSuffix);

			servletConfig.createErrorPage().errorCode("500").location("/500" + pageSuffix);

			servletConfig.createErrorPage().exceptionType("java.lang.Throwable").location("/500" + pageSuffix);

			servletConfig.createErrorPage().exceptionType("javax.faces.application.ViewExpiredException")
					.location("/expired" + pageSuffix);

			servletConfig.createErrorPage().exceptionType("javax.persistence.OptimisticLockException")
					.location("/optimistic" + pageSuffix);
		}
	}

	private void configOmniFaces(WebAppDescriptor servletConfig, FacesFacet facesFacet) {
		boolean found;
		found = servletConfig.getAllFilter().stream()
				.filter(f -> f.getFilterClass().equals("org.omnifaces.filter.GzipResponseFilter")).findAny()
				.isPresent();

		if (!found) {
			servletConfig.createFilter().filterName("gzipResponseFilter")
			.filterClass("org.omnifaces.filter.GzipResponseFilter")
			.createInitParam().paramName("threshold").paramValue("200");
		}

		FileResource<?> configFile = facesFacet.getConfigFile();

		Node node = XMLParser.parse(configFile.getResourceInputStream());
		Node applicationNode = node.getOrCreate("application");
		Optional<Node> combinedResourceHandler = applicationNode.getChildren().stream()
				.filter(f -> f.getName().equals("resource-handler") && f.getText().contains("CombinedResourceHandler"))
				.findFirst();

		if (!combinedResourceHandler.isPresent()) {
			applicationNode.createChild("resource-handler")
					.text("org.omnifaces.resourcehandler.CombinedResourceHandler");

			configFile.setContents(XMLParser.toXMLInputStream(node));
		}

	}

	private void configPrimeFaces(WebAppDescriptor servletConfig,
			List<ParamValueType<WebAppDescriptor>> allContextParam) {
		Optional<ParamValueType<WebAppDescriptor>> primefacesThemeParam = allContextParam.stream()
				.filter(c -> c.getParamValue().equals("primefaces.THEME")).findAny();

		if (!primefacesThemeParam.isPresent()) {
			servletConfig.createContextParam().paramName("primefaces.THEME").paramValue("admin");
		} else {
			primefacesThemeParam.get().paramValue("admin");
		}

		Optional<ParamValueType<WebAppDescriptor>> fontAwesomeParam = allContextParam.stream()
				.filter(c -> c.getParamValue().equals("primefaces.FONT_AWESOME")).findAny();

		if (!fontAwesomeParam.isPresent()) {
			servletConfig.createContextParam().paramName("primefaces.FONT_AWESOME").paramValue("true");
		} else {
			fontAwesomeParam.get().paramValue("true");
		}

	}

	private HashMap<Object, Object> getTemplateContext() {
		HashMap<Object, Object> context;
		context = new HashMap<>();
		context.put("templatePath", PAGE_TEMPLATE);
		return context;
	}

}
