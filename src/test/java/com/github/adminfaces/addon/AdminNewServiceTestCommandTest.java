package com.github.adminfaces.addon;

import com.github.adminfaces.addon.facet.AdminFacesFacet;
import com.github.adminfaces.addon.util.DependencyUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.github.adminfaces.addon.util.Constants.NEW_LINE;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(Arquillian.class)
public class AdminNewServiceTestCommandTest {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private ShellTest shellTest;

    private Project project;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class).addBeansXML().addPackages(true,
            "org.assertj.core");
    }

    @Before
    public void setUp() throws IOException, TimeoutException {
        //create project
        project = projectFactory.createTempProject(Arrays.asList(JavaEE7Facet.class, ServletFacet_3_1.class,
            JPAFacet.class, FacesFacet_2_0.class, JavaSourceFacet.class));
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        metadataFacet.setProjectGroupName("com.github.admin.addon");
        metadataFacet.setProjectName("AdminFaces");
        shellTest.clearScreen();
        //adminfaces setup
        shellTest.execute("jpa-setup --provider Hibernate --container JBOSS_EAP7 --db-type H2 --data-source-name java:jboss/datasources/ExampleDS", 30, TimeUnit.SECONDS);
        shellTest.execute("adminfaces-setup", 60, TimeUnit.SECONDS);
        shellTest.clearScreen();
        generateEntities();

        //setup adminfaces scaffold
        Result result = shellTest.execute("scaffold-setup --provider AdminFaces", 60, TimeUnit.SECONDS);
        if (result instanceof Failed) {
            ((Failed) result).getException().printStackTrace();
        }
        assertThat(result).isInstanceOf(CompositeResult.class).extracting("message")
                .contains("***SUCCESS*** Scaffold was setup successfully.");


        //scaffold from entities
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        String entityPackageName = sourceFacet.getBasePackage() + ".model";

        Result scaffoldGenerate1 = shellTest
                .execute(("scaffold-generate --entities " + entityPackageName + ".*"), 2, TimeUnit.MINUTES);

        if (scaffoldGenerate1 instanceof Failed) {
            ((Failed) scaffoldGenerate1).getException().printStackTrace();
        }
        assertThat(scaffoldGenerate1).isNotInstanceOf(Failed.class);

        //setup admin test harness
        shellTest.clearScreen();
        Result testHarnessSetupResult = shellTest
                .execute("adminfaces-test-harness-setup", 1, TimeUnit.MINUTES);
        if (testHarnessSetupResult instanceof Failed) {
            ((Failed) testHarnessSetupResult).getException().printStackTrace();
        }
        assertThat(testHarnessSetupResult).isInstanceOf(Result.class).extracting("message")
                .contains("AdminFaces test harness setup finished successfully!");
    }

    @Test
    public void shouldCreateServiceTests() throws IOException, TimeoutException {
        shellTest.clearScreen();
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        String entityPackageName = sourceFacet.getBasePackage() + ".service";

        Result testHarnessSetupResult = shellTest
                .execute("adminfaces-new-service-test", 1, TimeUnit.MINUTES);
        if (testHarnessSetupResult instanceof Failed) {
            ((Failed) testHarnessSetupResult).getException().printStackTrace();
        }
        assertThat(testHarnessSetupResult).isInstanceOf(Result.class).extracting("message")
                .contains("Service test(s) created successfully!");
    }

    @After
    public void tearDown() throws Exception {
        shellTest.close();
    }

    private void generateEntities() throws TimeoutException {
        shellTest.execute("jpa-new-entity --named Talk", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named title", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named description --length 2000", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named date --type java.util.Date --temporal-type DATE", 10, TimeUnit.SECONDS);

        shellTest.execute("constraint-add --on-property title --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property description --constraint Size --max 2000", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property date --constraint NotNull", 10, TimeUnit.SECONDS);

        shellTest.execute("jpa-new-entity --named Room", 15, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named name --length 20", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named capacity --type java.lang.Short", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named hasWifi --type java.lang.Boolean", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named talks --type Talk --relationship-type One-to-Many", 10, TimeUnit.SECONDS);

        shellTest.execute("constraint-add --on-property name --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property capacity --constraint NotNull", 10, TimeUnit.SECONDS);

        shellTest.execute("jpa-new-embeddable --named Address", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named street --length 50 --not-nullable", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named city  --length 50 --not-nullable", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named zipcode --columnName --length 10 --not-nullable --type java.lang.Integer", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named state", 10, TimeUnit.SECONDS);

        shellTest.execute("jpa-new-entity --named Speaker", 15, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named firstname", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named surname", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named bio --length 2000", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named twitter", 10, TimeUnit.SECONDS);

        shellTest.execute("jpa-new-field --named talks --type Talk --relationship-type One-to-Many --inverse-field-name speaker", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named address --entity --type Address --relationship-type Embedded", 10, TimeUnit.SECONDS);

        shellTest.execute("constraint-add --on-property firstname --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property surname --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property bio --constraint Size --max 2000", 10, TimeUnit.SECONDS);

        shellTest.execute("cd ../Talk.java", 15, TimeUnit.SECONDS);
        //shellTest.execute("jpa-new-field --named speaker --type Speaker --relationship-type Many-to-One", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named room --type com.github.admin.addon.model.Room --relationship-type Many-to-One", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property speaker --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property room --constraint NotNull", 10, TimeUnit.SECONDS);
    }

}
