package com.github.admin.addon;

import com.github.admin.addon.facet.AdminFacet;
import com.github.admin.addon.util.DependencyUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.After;

import static com.github.admin.addon.TestUtil.newLine;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.assertj.core.api.Assertions.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;


@RunWith(Arquillian.class)
public class AdminSetupCommandTest {

  @Deployment
  @AddonDependencies
  public static AddonArchive getDeployment() {
    return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClass(TestUtil.class).addPackages(true, "org.assertj.core");
  }

  @Inject
  private ProjectFactory projectFactory;

  @Inject
  private ShellTest      shellTest;

  private Project        project;

  @Before
  public void setUp() throws IOException {
    project = projectFactory.createTempProject(Arrays.asList(JavaEE7Facet.class, ServletFacet_3_1.class, JPAFacet.class, FacesFacet_2_0.class, JavaSourceFacet.class));
    MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
    metadataFacet.setProjectGroupName("com.github.admin.addon");
    metadataFacet.setProjectName("AdminFaces");

    shellTest.clearScreen();
  }

  @After
  public void tearDown() throws Exception {
    shellTest.close();
  }


  @Test
  public void shouldSetupAdminFaces() throws Exception {
    shellTest.getShell().setCurrentResource(project.getRoot());
    Result result = shellTest.execute("adminfaces-setup", 50, TimeUnit.SECONDS);
    assertThat(result).isNotNull().isNotInstanceOf(Failed.class);

    List<Result> results = ((CompositeResult) result).getResults();

    assertThat(results).isNotEmpty();

    assertThat(results.get(0)).extracting("message").contains("AdminFaces setup completed successfully!");

    project = projectFactory.findProject(project.getRoot());
    assertThat(project.hasFacet(AdminFacet.class)).isTrue();
    DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);

    assertThat(dependencyFacet.hasDirectDependency(DependencyBuilder.create().setCoordinate(DependencyUtil.ADMIN_TEMPLATE_COORDINATE))).isTrue();

    Resource<?> projectRoot = project.getRoot();
    File adminConfig = new File(projectRoot.getFullyQualifiedName() + "/src/main/resources/admin-config.properties");
    assertThat(adminConfig).exists().hasContent("admin.renderControlSidebar=true" + newLine() + "admin.controlSidebar.showOnMobile=true" + newLine() + "admin.ignoredResources=rest");
    assertThat(new File(projectRoot.getFullyQualifiedName() + "/src/main/resources/messages.properties")).exists();
    WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
    assertThat(new File(web.getWebResource("index.xhtml").getFullyQualifiedName())).exists();
    assertThat(new File(web.getWebResource("login.xhtml").getFullyQualifiedName())).exists();
    assertThat(new File(web.getWebResource("WEB-INF/faces-config.xml").getFullyQualifiedName())).exists();
    assertThat(new File(web.getWebResource("WEB-INF/web.xml").getFullyQualifiedName())).exists();
    assertThat(new File(web.getWebResource("includes/menu.xhtml").getFullyQualifiedName())).exists();
    assertThat(new File(web.getWebResource("includes/menubar.xhtml").getFullyQualifiedName())).exists();
    assertThat(new File(web.getWebResource("includes/top-bar.xhtml").getFullyQualifiedName())).exists();

    File template = new File(web.getWebResource("WEB-INF/templates/template.xhtml").getFullyQualifiedName());
    assertThat(template).exists();
    assertThat(contentOf(template))
    .contains("        <title>Admin Faces</title>")
    .contains("    <ui:define name=\"logo\">"+newLine() +
"        Admin Faces"+newLine() +
"    </ui:define>")
    .contains("    <ui:define name=\"logo-mini\">"+newLine() +
"         Adm"+newLine() +
"    </ui:define>");
    assertThat(new File(web.getWebResource("WEB-INF/templates/template-top.xhtml").getFullyQualifiedName())).exists();

    assertThat(project.getFacet(AdminFacet.class).isInstalled()).isTrue();
  }

}
