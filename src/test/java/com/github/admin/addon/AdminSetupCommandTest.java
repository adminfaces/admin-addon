package com.github.admin.addon;

import com.github.admin.addon.facet.AdminFacet;
import com.github.admin.addon.ui.AdminSetupCommand;
import com.github.admin.addon.util.DependencyUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.JavaEEFacet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.controller.CommandController;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.test.UITestHarness;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.After;

import static com.github.admin.addon.TestUtil.newLine;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;

import static org.junit.Assert.*;

@RunWith(Arquillian.class)
public class AdminSetupCommandTest {

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class)
                .addBeansXML()
                .addClass(TestUtil.class)
                .addPackages(true,"org.assertj.core");
    }

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private UITestHarness uiTestHarness;

    @Inject
    private ShellTest shellTest;

    private Project project;

    @Before
    public void setUp() throws IOException {
        project = projectFactory.createTempProject(Arrays.asList(JavaEE7Facet.class,ServletFacet_3_1.class, JPAFacet.class, JavaSourceFacet.class));
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        metadataFacet.setProjectGroupName("com.github.admin.addon");

        shellTest.clearScreen();
    }

    @After
    public void tearDown() throws Exception {
        shellTest.close();
    }

    @Test
    public void checkCommandMetadata() throws Exception {
        try (CommandController controller = uiTestHarness.createCommandController(AdminSetupCommand.class, project.getRoot())) {
            controller.initialize();
            // Checks the command metadata
            assertTrue(controller.getCommand() instanceof AdminSetupCommand);
            UICommandMetadata metadata = controller.getMetadata();
            assertEquals("AdminFaces: Setup", metadata.getName());
            assertEquals("AdminFaces", metadata.getCategory().getName());
            assertNull(metadata.getCategory().getSubCategory());
            assertEquals(1, controller.getInputs().size());
        }
    }


    @Test
    public void checkCommandShellAdminSetup() throws Exception {
        shellTest.getShell().setCurrentResource(project.getRoot());
        Result result = shellTest.execute("adminfaces-setup", 25, TimeUnit.SECONDS);
        assertThat(result).isNotNull()
                .isNotInstanceOf(Failed.class)
                .extracting("message")
                .contains("AdminFaces setup completed successfully!");

        project = projectFactory.findProject(project.getRoot());
        assertThat(project.hasFacet(AdminFacet.class)).isTrue();
        DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);

        assertThat(dependencyFacet.hasDirectDependency(DependencyBuilder.create()
                .setCoordinate(DependencyUtil.ADMIN_TEMPLATE_COORDINATE))).isTrue();

        Resource<?> projectRoot = project.getRoot();
        File adminConfig = new File(projectRoot.getFullyQualifiedName()+"/src/main/resources/admin-config.properties");
        assertThat(adminConfig)
                .exists()
                .hasContent("admin.renderControlSidebar=true" + newLine() +
                		"admin.controlSidebar.showOnMobile=truel"+newLine() +
                        "admin.ignoredResources=rest");

        File includesDir  = new File(projectRoot.getFullyQualifiedName()+"/src/main/webapp/includes");
        assertThat(new File(includesDir.getAbsolutePath()+"/menu.xhtml")).exists();
        assertThat(new File(includesDir.getAbsolutePath()+"/menubar.xhtml")).exists();
        assertThat(new File(includesDir.getAbsolutePath()+"/top-bar.xhtml")).exists();

        assertThat(project.getFacet(AdminFacet.class).isInstalled()).isTrue();
    }

    /*@Test
    public void testSwaggerSetup() throws Exception {
        try (CommandController controller = uiTestHarness.createCommandController(AdminSetupWizard.class, project.getRoot())) {
            controller.initialize();
            Assert.assertTrue(controller.isValid());
            final AtomicBoolean flag = new AtomicBoolean();
            controller.getContext().addCommandExecutionListener(new AbstractCommandExecutionListener() {
                @Override
                public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result) {
                    if (result.getMessage().equals("Swagger setup completed successfully!")) {
                        flag.set(true);
                    }
                }
            });
            controller.execute();
            Assert.assertTrue(flag.get());
            project = projectFactory.findProject(project.getRoot());
            AdminFacet facet = project.getFacet(AdminFacet.class);
            Assert.assertTrue(facet.isInstalled());

            MavenPluginAdapter swaggerPlugin = (MavenPluginAdapter) project.getFacet(MavenPluginFacet.class)
                    .getEffectivePlugin(AdminFacetImpl.ANALYZER_PLUGIN_COORDINATE);
            Assert.assertEquals("jaxrs-analyzer-maven-plugin", swaggerPlugin.getCoordinate().getArtifactId());
            Assert.assertEquals(1, swaggerPlugin.getExecutions().size());
            PluginExecution exec = swaggerPlugin.getExecutions().get(0);
            assertEquals(exec.getGoals().get(0), "analyze-jaxrs");
            assertEquals(exec.getPhase(), AdminFacetImpl.ANALYZER_PHASE);
        }
    }

    @Test
    public void testSwaggerSetupWithParameters() throws Exception {
        try (CommandController controller = uiTestHarness.createCommandController(AdminSetupWizard.class, project.getRoot())) {
            controller.initialize();
            controller.setValueFor("resourcesDir", "apidocs");
            Assert.assertTrue(controller.isValid());

            final AtomicBoolean flag = new AtomicBoolean();
            controller.getContext().addCommandExecutionListener(new AbstractCommandExecutionListener() {
                @Override
                public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result) {
                    if (result.getMessage().equals("Swagger setup completed successfully!")) {
                        flag.set(true);
                    }
                }
            });
            controller.execute();
            Assert.assertTrue(flag.get());
            project = projectFactory.findProject(project.getRoot());
            AdminFacet facet = project.getFacet(AdminFacet.class);
            Assert.assertTrue(facet.isInstalled());

            MavenPluginAdapter swaggerPlugin = (MavenPluginAdapter) project.getFacet(MavenPluginFacet.class)
                    .getEffectivePlugin(AdminFacetImpl.ANALYZER_PLUGIN_COORDINATE);
            Assert.assertEquals("jaxrs-analyzer-maven-plugin", swaggerPlugin.getCoordinate().getArtifactId());
            Assert.assertEquals(1, swaggerPlugin.getExecutions().size());
            PluginExecution exec = swaggerPlugin.getExecutions().get(0);
            assertEquals(exec.getGoals().get(0), AdminFacetImpl.ANALYZER_GOAL);
            Xpp3Dom pluginConfig = (Xpp3Dom) swaggerPlugin.getConfiguration();
            assertEquals(pluginConfig.getChildCount(), 2);
            assertEquals(pluginConfig.getChild("backend").getValue(), "swagger");
            String projectFinalName = project.getFacet(MavenFacet.class).getModel().getBuild().getFinalName();
            assertEquals(pluginConfig.getChild("resourcesDir").getValue(), projectFinalName + "/apidocs");
        }
    }

    @Test
    public void testSwaggerSetupWithNullParameters() throws Exception {
        try (CommandController controller = uiTestHarness.createCommandController(AdminSetupWizard.class, project.getRoot())) {
            controller.initialize();
            controller.setValueFor("resourcesDir", null);
            assertTrue(controller.isValid());

            final AtomicBoolean flag = new AtomicBoolean();
            controller.getContext().addCommandExecutionListener(new AbstractCommandExecutionListener() {
                @Override
                public void postCommandExecuted(UICommand command, UIExecutionContext context, Result result) {
                    if (result.getMessage().equals("Swagger setup completed successfully!")) {
                        flag.set(true);
                    }
                }
            });
            controller.execute();
            assertTrue(flag.get());
            project = projectFactory.findProject(project.getRoot());
            AdminFacet facet = project.getFacet(AdminFacet.class);
            assertTrue(facet.isInstalled());

            MavenPluginAdapter swaggerPlugin = (MavenPluginAdapter) project.getFacet(MavenPluginFacet.class)
                    .getEffectivePlugin(AdminFacetImpl.ANALYZER_PLUGIN_COORDINATE);
            assertEquals("jaxrs-analyzer-maven-plugin", swaggerPlugin.getCoordinate().getArtifactId());
            assertEquals(1, swaggerPlugin.getExecutions().size());
            PluginExecution analyzerExecution = swaggerPlugin.getExecutions().get(0);
            assertEquals(AdminFacetImpl.ANALYZER_GOAL, analyzerExecution.getGoals().get(0));
            Xpp3Dom pluginConfig = (Xpp3Dom) swaggerPlugin.getConfiguration();
            String projectFinalName = project.getFacet(MavenFacet.class).getModel().getBuild().getFinalName();
            assertEquals(pluginConfig.getChild("backend").getValue(), "swagger");
            assertEquals(pluginConfig.getChild("resourcesDir").getValue(), projectFinalName + "/apidocs");
        }
    }*/

}
