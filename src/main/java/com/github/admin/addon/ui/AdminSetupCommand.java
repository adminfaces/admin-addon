package com.github.admin.addon.ui;

import static org.jboss.forge.addon.scaffold.util.ScaffoldUtil.createOrOverwrite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.shrinkwrap.descriptor.api.javaee.ParamValueCommonType;
import org.metawidget.util.simple.StringUtils;

import com.github.admin.addon.config.AdminConfiguration;
import com.github.admin.addon.facet.AdminFacet;
import com.github.admin.addon.freemarker.FreemarkerTemplateProcessor;
import com.github.admin.addon.freemarker.TemplateFactory;
import java.time.Year;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.shrinkwrap.descriptor.api.javaee7.ParamValueType;
import org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor;

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
	private static final String SCAFFOLD_RESOURCES = "/scaffold";
	private static final Logger LOG = Logger.getLogger(AdminSetupCommand.class.getName());

	@Inject
	private FacetFactory facetFactory;

	@Inject
	private ProjectFactory projectFactory;

	@Inject
	private AdminConfiguration adminConfig;

	@Inject
	private TemplateFactory templates;

	@Inject
	@WithAttributes(label = "Logo mini", description = "Logo mini is shown on small screens on the top left of the page. Defaults to the first 3 letters of application name.")
	private UIInput<String> logoMini;

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
	  
		final Project project = getSelectedProject(context) != null ? getSelectedProject(context) : getSelectedProject(context.getUIContext());
		 
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
        
        if(!project.hasFacet(ServletFacet_3_1.class)) {
            ServletFacet_3_1 servletFacet_3_1 = facetFactory.create(project, ServletFacet_3_1.class);
            facetFactory.install(project, servletFacet_3_1);
        }

		MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
		String projectName = metadataFacet.getProjectName();
		adminConfig.setProjectName(projectName);

		String logoMini = resolveLogoMini(this.logoMini.getValue(), projectName);

		adminConfig.setLogoMini(logoMini);
		addAdminFacesResources(project).forEach(r -> results.add(Results.success("Added " + r.getFullyQualifiedName().replace(project.getRoot().getFullyQualifiedName(), ""))));
		setupWebXML(project);

		return Results.aggregate(results);

	}

	private String resolveLogoMini(String logoMini, String projectName) {
		if (logoMini == null || "".equals(logoMini.trim())) {
			if (projectName.length() > 3) {
				logoMini = projectName.substring(0, 3);
			} else {
				logoMini = projectName;
			}
		}
		return logoMini;
	}

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
         Project project = getSelectedProject(builder.getUIContext());
         MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
		 String projectName = metadataFacet.getProjectName();
         logoMini.setDefaultValue(resolveLogoMini("", projectName));
		 builder.add(logoMini);
	}

	@SuppressWarnings("rawtypes")
	protected List<Resource<?>> addAdminFacesResources(Project project) {
		List<Resource<?>> result = new ArrayList<>();
		WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
		JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);

		AdminFacet adminFacet = project.getFacet(AdminFacet.class);
		AdminConfiguration adminConfig = adminFacet.getConfiguration();

		ServletFacet_3_1 servlet = project.getFacet(ServletFacet_3_1.class);
        
        org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor servletConfig = (org.jboss.shrinkwrap.descriptor.api.webapp31.WebAppDescriptor) servlet
                .getConfig();
        servletConfig.getOrCreateWelcomeFileList().welcomeFile(INDEX_HTML);

		HashMap<Object, Object> context = getTemplateContext();
		context.put("appName", StringUtils.uncamelCase(adminConfig.getProjectName()));
		context.put("logoMini", adminConfig.getLogoMini());
        context.put("copyrightYear", Year.now().getValue());

		
		//admin config
		addAdminConfig(project);
		// Basic pages

		result.add(createOrOverwrite(web.getWebResource(INDEX_PAGE),
				FreemarkerTemplateProcessor.processTemplate(context, templates.getIndexTemplate())));

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

		// beans

		MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);

		// logon
		try (InputStream logonStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("/infra/security/LogonMB.java")) {
			JavaSource<?> logonMB = (JavaSource<?>) Roaster.parse(logonStream);
			logonMB.setPackage(metadataFacet.getProjectGroupName() + ".infra");
			javaSource.saveJavaSource(logonMB);
			FileUtils.copyInputStreamToFile(logonStream,
					new File(project.getRoot().getFullyQualifiedName()
							+ logonMB.getPackage().replaceAll("\\.", "/")));
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

		configOmniFaces(servletConfig);

		servlet.saveConfig(servletConfig);
	}

	private void configOmniFaces(WebAppDescriptor servletConfig) {
		boolean found;
		found = servletConfig.getAllFilter().stream()
				.filter(f -> f.getFilterClass().equals("org.omnifaces.filter.GzipResponseFilter")).findAny()
				.isPresent();

		if (!found) {
			servletConfig.createFilter().filterClass("org.omnifaces.filter.GzipResponseFilter")
					.filterName("gzipResponseFilter").createInitParam().paramName("threshold").paramValue("200");
		}
	}

	private void configPrimeFaces(WebAppDescriptor servletConfig, List<ParamValueType<WebAppDescriptor>> allContextParam) {
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
