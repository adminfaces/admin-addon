package com.github.adminfaces.addon;

import static com.github.adminfaces.addon.util.Constants.NEW_LINE;
import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.*;
import com.github.adminfaces.addon.util.TestUtils;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import java.io.File;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.arquillian.AddonDependencies;
import org.junit.Test;

@RunWith(Arquillian.class)
public class AdminScaffoldConfigCommandTest {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private ShellTest shellTest;

    private Project project;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class).addBeansXML()
            .addPackages(true, "org.assertj.core").addClass(TestUtils.class)
            .addAsResource(AdminScaffoldConfigCommandTest.class.getResource("/scaffolded-app.zip"), "scaffolded-app.zip");
    }

    @Before
    public void setUp() throws IOException, TimeoutException, NoSuchFieldException, IllegalArgumentException, IllegalArgumentException, IllegalAccessException, IllegalAccessException {
        project = projectFactory.createTempProject();
        TestUtils.unzip(getClass().getResourceAsStream("/scaffolded-app.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
    }

    @Test
    public void shouldEditGlobalConfigViaScaffoldConfigCommand() throws TimeoutException, IOException {
        project = projectFactory.findProject(project.getRoot());
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
            .contains("!!com.github.adminfaces.addon.scaffold.model.GlobalConfig" + NEW_LINE
                + "datatableEditable: true" + NEW_LINE
                + "datatableReflow: true" + NEW_LINE
                + "dateComponentType: " + CALENDAR.name()
                + "" + NEW_LINE
                + "inputSize: 30" + NEW_LINE
                + "menuIcon: fa fa-edit" + NEW_LINE
                + "toManyComponentType: " + SELECT_MANY_MENU.name()
                + "" + NEW_LINE
                + "toOneComponentType: " + SELECT_ONE_MENU.name()
                + "");
    }

    @Test
    public void shouldEditEntityConfigViaScaffoldConfigCommand() throws TimeoutException, IOException {
        shellTest.clearScreen();
        project = projectFactory.findProject(project.getRoot());
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        Resource<?> scaffoldDir = resourcesFacet.getResourceDirectory().getChild("scaffold");
        FileResource<?> talkScaffoldConfig = scaffoldDir.getChild("Talk.yml").reify(FileResource.class);
        StringBuilder scaffoldConfigCommand = new StringBuilder("adminfaces-scaffold-config --config-file ")
            .append(talkScaffoldConfig.getFullyQualifiedName()).append(" --choice-field-to-change date --type ").append(DATEPICKER.name())
            .append(" --datatable-editable --menu-icon \"fa fa-edit\"");
        Result scaffoldConfigResult = shellTest.execute(scaffoldConfigCommand.toString(), 5, TimeUnit.MINUTES);
        if (scaffoldConfigResult instanceof Failed) {
            ((Failed) scaffoldConfigResult).getException().printStackTrace();
        }

        scaffoldConfigCommand = new StringBuilder("adminfaces-scaffold-config --config-file ")
            .append(talkScaffoldConfig.getFullyQualifiedName()).append(" --choice-field-to-change description --required ");
        scaffoldConfigResult = shellTest.execute(scaffoldConfigCommand.toString(), 5, TimeUnit.MINUTES);
        if (scaffoldConfigResult instanceof Failed) {
            ((Failed) scaffoldConfigResult).getException().printStackTrace();
        }

        File talkEntityConfigFile = new File(talkScaffoldConfig.getFullyQualifiedName());
        assertThat(contentOf(talkEntityConfigFile))
            .contains("!!com.github.adminfaces.addon.scaffold.model.EntityConfig"+NEW_LINE +
"datatableEditable: true"+NEW_LINE +
"datatableReflow: true"+NEW_LINE +
"displayField: title"+NEW_LINE +
"fields:"+NEW_LINE +
"- hidden: false"+NEW_LINE +
"  length: 100"+NEW_LINE +
"  name: id"+NEW_LINE +
"  required: true"+NEW_LINE +
"  type: INPUT_NUMBER"+NEW_LINE +
"- hidden: false"+NEW_LINE +
"  length: 100"+NEW_LINE +
"  name: version"+NEW_LINE +
"  required: false"+NEW_LINE +
"  type: INPUT_NUMBER"+NEW_LINE +
"- hidden: false"+NEW_LINE +
"  length: 100"+NEW_LINE +
"  name: title"+NEW_LINE +
"  required: true"+NEW_LINE +
"  type: INPUT_TEXT"+NEW_LINE +
"- hidden: false"+NEW_LINE +
"  length: 2000"+NEW_LINE +
"  name: description"+NEW_LINE +
"  required: true"+NEW_LINE +
"  type: TEXT_AREA"+NEW_LINE +
"- hidden: false"+NEW_LINE +
"  length: 100"+NEW_LINE +
"  name: date"+NEW_LINE +
"  required: true"+NEW_LINE +
"  type: DATEPICKER"+NEW_LINE +
"- hidden: false"+NEW_LINE +
"  length: 100"+NEW_LINE +
"  name: speaker"+NEW_LINE +
"  required: true"+NEW_LINE +
"  type: AUTOCOMPLETE"+NEW_LINE +
"- hidden: false"+NEW_LINE +
"  length: 100"+NEW_LINE +
"  name: room"+NEW_LINE +
"  required: true"+NEW_LINE +
"  type: AUTOCOMPLETE"+NEW_LINE +
"menuIcon: fa fa-circle-o");
    }

}
