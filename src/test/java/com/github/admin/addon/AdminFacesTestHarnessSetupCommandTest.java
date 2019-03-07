package com.github.admin.addon;

import com.github.adminfaces.addon.facet.AdminFacesTestHarnessFacet;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.TimeoutException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.addon.javaee.jpa.JPAFacet_2_1;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.arquillian.AddonDependencies;
import org.junit.Test;

@RunWith(Arquillian.class)
public class AdminFacesTestHarnessSetupCommandTest {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private ShellTest shellTest;

    private Project project;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class).addBeansXML()
            .addPackages(true, "org.assertj.core");
    }

    @Before
    public void setUp() throws IOException, TimeoutException {
        project = projectFactory.createTempProject(Arrays.asList(JavaEE7Facet.class, ServletFacet_3_1.class,
            JPAFacet_2_1.class, FacesFacet_2_0.class, JavaSourceFacet.class));
        shellTest.getShell().setCurrentResource(project.getRoot());
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        metadataFacet.setProjectGroupName("com.github.admin.addon");
        metadataFacet.setProjectName("AdminFaces");
        metadataFacet.setProjectVersion("1.0");
        shellTest.execute("jpa-setup --provider Hibernate --container JBOSS_EAP7 --db-type H2 --data-source-name java:jboss/datasources/ExampleDS", 30, TimeUnit.SECONDS);
        shellTest.execute("adminfaces-setup", 60, TimeUnit.SECONDS);
    }

    @Test
    public void shouldSetUpAdminFacesTestHarness() throws TimeoutException, IOException {
        shellTest.clearScreen();
        Result testHarnessSetupResult = shellTest
            .execute("adminfaces-test-harness-setup", 5, TimeUnit.MINUTES);
        if (testHarnessSetupResult instanceof Failed) {
            ((Failed) testHarnessSetupResult).getException().printStackTrace();
        }
        assertThat(testHarnessSetupResult).isInstanceOf(Result.class).extracting("message")
            .contains("AdminFaces test harness setup finished successfully!");
        project = projectFactory.findProject(project.getRoot());
        assertThat(project.hasFacet(AdminFacesTestHarnessFacet.class)).isTrue();
        MavenFacet maven = project.getFacet(MavenFacet.class);
        boolean buildSuccess = maven.executeMaven(Arrays.asList("clean", "package"));
        assertThat(buildSuccess).isTrue();
    }
    

}
