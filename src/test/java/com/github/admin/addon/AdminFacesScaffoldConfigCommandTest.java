package com.github.admin.addon;

import static com.github.adminfaces.addon.util.Constants.NEW_LINE;
import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.*;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import java.io.File;
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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.addon.javaee.jpa.JPAFacet_2_1;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.arquillian.AddonDependencies;
import org.junit.Test;

@RunWith(Arquillian.class)
public class AdminFacesScaffoldConfigCommandTest {

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
    public void setUp() throws IOException, TimeoutException, NoSuchFieldException, IllegalArgumentException, IllegalArgumentException, IllegalAccessException, IllegalAccessException {
        project = projectFactory.createTempProject(Arrays.asList(JavaEE7Facet.class, ServletFacet_3_1.class,
            JPAFacet_2_1.class, FacesFacet_2_0.class, JavaSourceFacet.class));
        shellTest.getShell().setCurrentResource(project.getRoot());
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        metadataFacet.setProjectGroupName("com.github.admin.addon");
        metadataFacet.setProjectName("AdminFaces");
        metadataFacet.setProjectVersion("1.0");
        shellTest.execute("jpa-setup --provider Hibernate --container JBOSS_EAP7 --db-type H2 --data-source-name java:jboss/datasources/ExampleDS", 30, TimeUnit.SECONDS);
        shellTest.execute("adminfaces-setup", 60, TimeUnit.SECONDS);
        shellTest.clearScreen();
        generateEntities();
        Result result = shellTest.execute("scaffold-setup --provider AdminFaces", 10, TimeUnit.MINUTES);
        if (result instanceof Failed) {
            ((Failed) result).getException().printStackTrace();
        }
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        String entityPackageName = sourceFacet.getBasePackage() + ".model";
        Result scaffoldGenerate1 = shellTest
            .execute(("scaffold-generate --entities " + entityPackageName + ".*"), 1, TimeUnit.MINUTES);
        if (scaffoldGenerate1 instanceof Failed) {
            ((Failed) scaffoldGenerate1).getException().printStackTrace();
        }
    }

    @Test
    public void shouldEditGlobalConfigViaScaffoldConfigCommand() throws TimeoutException, IOException {
        shellTest.clearScreen();
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        Resource<?> scaffoldDir = resourcesFacet.getResourceDirectory().getChild("scaffold");
        FileResource<?> globalScaffoldConfig = scaffoldDir.getChild("global-config.yml").reify(FileResource.class);
        StringBuilder scaffoldConfigCommand = new StringBuilder("adminfaces-scaffold-config --config-file ")
            .append(globalScaffoldConfig.getFullyQualifiedName()).append(" --input-size 30 --to-one-component-type ")
            .append(SELECT_ONE_MENU.name()).append(" --to-many-component-type ").append(SELECT_MANY_MENU.name())
            .append(" --datatable-editable --menu-icon \"fa fa-edit\"");
        Result scaffoldConfigResult = shellTest.execute(scaffoldConfigCommand.toString(), 5, TimeUnit.MINUTES);
        if (scaffoldConfigResult instanceof Failed) {
            ((Failed) scaffoldConfigResult).getException().printStackTrace();
        }
        File globalConfigFile = new File(globalScaffoldConfig.getFullyQualifiedName());
        assertThat(contentOf(globalConfigFile))
            .contains("!!com.github.adminfaces.addon.scaffold.model.GlobalConfig"+ NEW_LINE
                + "datatableEditable: true"+ NEW_LINE
                + "datatableReflow: true"+ NEW_LINE
                + "dateComponentType: " + CALENDAR.name()
                        + ""+ NEW_LINE
                + "inputSize: 30"+ NEW_LINE
                + "menuIcon: fa fa-edit"+ NEW_LINE
                + "toManyComponentType: " + SELECT_MANY_MENU.name()
                + ""+ NEW_LINE
                + "toOneComponentType: " + SELECT_ONE_MENU.name()
                + "");
    }

    private void generateEntities() throws TimeoutException {
        shellTest.execute("jpa-new-entity --named Talk", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named title", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named description --length 2000", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named date --type java.util.Date --temporal-type DATE", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property title --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property description --constraint Size --max 2000", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property date --constraint NotNull", 10, TimeUnit.SECONDS);

    }

}
