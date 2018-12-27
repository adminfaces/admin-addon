package com.github.admin.addon;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFacet;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

/**
 * Test class for {@link com.github.adminfaces.addon.scaffold.AdminFacesScaffoldProvider}
 *
 * @author <a href="github.com/rmpestano">Rafael Pestano</a>
 */
@RunWith(Arquillian.class)
public class AdminFacesScaffoldTest {

    @Inject
    ProjectFactory projectFactory;

    @Inject
    ShellTest shellTest;

    Project project;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClass(TestUtil.class).addPackages(true,
                "org.assertj.core");
    }

    @Before
    public void setUp() throws IOException, TimeoutException {
        project = projectFactory.createTempProject(Arrays.asList(JavaEE7Facet.class, ServletFacet_3_1.class,
                JPAFacet.class, FacesFacet_2_0.class, JavaSourceFacet.class));
        shellTest.getShell().setCurrentResource(project.getRoot());
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        metadataFacet.setProjectGroupName("com.github.admin.addon");
        metadataFacet.setProjectName("AdminFaces");
        shellTest.execute("adminfaces-setup", 60, TimeUnit.SECONDS);
        shellTest.clearScreen();
    }

    @Test
    public void testScaffoldSetup() throws Exception {
        shellTest.execute("jpa-new-entity --named Customer", 15, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named firstName", 10, TimeUnit.SECONDS);
        Result result = shellTest.execute("scaffold-setup --provider AdminFaces", 10, TimeUnit.SECONDS);
        assertThat(result).isInstanceOf(CompositeResult.class)
                .extracting("message")
                .contains("Scaffold was setup successfully");
        Assert.assertThat(result, is(instanceOf(CompositeResult.class)));
    }

   /*@Test
   public void shouldCreateOneErrorPageForEachErrorCode() throws Exception
   {
      shellTest.execute("servlet-setup --servlet-version 3.1", 10, TimeUnit.SECONDS);
      shellTest.execute("jpa-setup", 10, TimeUnit.SECONDS);
      shellTest.execute("jpa-new-entity --named Customer", 10, TimeUnit.SECONDS);
      shellTest.execute("jpa-new-field --named firstName", 10, TimeUnit.SECONDS);
      shellTest.execute("jpa-new-entity --named Publisher", 10, TimeUnit.SECONDS);
      shellTest.execute("jpa-new-field --named firstName", 10, TimeUnit.SECONDS);
      Result result = shellTest.execute("scaffold-setup --provider Faces", 10, TimeUnit.SECONDS);
      Assert.assertThat(result, not(instanceOf(Failed.class)));
      Project project = projectFactory.findProject(shellTest.getShell().getCurrentResource());
      Assert.assertTrue(project.hasFacet(ServletFacet_3_1.class));
      ServletFacet_3_1 servletFacet = project.getFacet(ServletFacet_3_1.class);
      Assert.assertNotNull(servletFacet.getConfig());

      String entityPackageName = project.getFacet(JavaSourceFacet.class).getBasePackage() + ".model";
      Result scaffoldGenerate1 = shellTest
               .execute(("scaffold-generate --web-root /admin --targets " + entityPackageName
                        + ".Customer"), 10,
                        TimeUnit.SECONDS);
      Assert.assertThat(scaffoldGenerate1, not(instanceOf(Failed.class)));

      Assert.assertEquals(2, servletFacet.getConfig().getAllErrorPage().size());

      Result scaffoldGenerate2 = shellTest
               .execute(("scaffold-generate --web-root /admin --targets " + entityPackageName
                        + ".Publisher"), 10,
                        TimeUnit.SECONDS);
      Assert.assertThat(scaffoldGenerate2, not(instanceOf(Failed.class)));
      Assert.assertEquals(2, servletFacet.getConfig().getAllErrorPage().size());
   }

   @Test
   public void shouldScaffoldEntity() throws Exception
   {
      Assert.assertThat(shellTest.execute("javaee-setup --java-ee-version 7", 10, TimeUnit.SECONDS),
               not(instanceOf(Failed.class)));
      Assert.assertThat(shellTest.execute("jpa-setup", 10, TimeUnit.SECONDS), not(instanceOf(Failed.class)));
      Assert.assertThat(shellTest.execute("jpa-new-entity --named Customer", 10, TimeUnit.SECONDS),
               not(instanceOf(Failed.class)));
      Assert.assertThat(shellTest.execute("jpa-new-field --named firstName", 10, TimeUnit.SECONDS),
               not(instanceOf(Failed.class)));
      Result result = shellTest.execute("scaffold-setup --provider Faces", 10, TimeUnit.SECONDS);
      Assert.assertThat(result, not(instanceOf(Failed.class)));

      Project project = projectFactory.findProject(shellTest.getShell().getCurrentResource());
      String entityPackageName = project.getFacet(JavaSourceFacet.class).getBasePackage() + ".model";
      result = shellTest.execute(
               "scaffold-generate --provider Faces --targets " + entityPackageName + ".Customer", 10, TimeUnit.SECONDS);
      Assert.assertThat(result, not(instanceOf(Failed.class)));
   }*/

    @After
    public void tearDown() throws Exception {
        if (project != null) {
            project.getRoot().delete(true);
        }
        shellTest.close();
    }
}
