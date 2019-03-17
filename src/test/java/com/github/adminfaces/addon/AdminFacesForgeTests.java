/*
 * The MIT License
 *
 * Copyright 2019 rafael-pestano.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adminfaces.addon;

import com.github.adminfaces.addon.facet.AdminFacesFacet;
import com.github.adminfaces.addon.facet.AdminFacesTestHarnessFacet;
import com.github.adminfaces.addon.scaffold.config.ScaffoldConfigLoader;
import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.CALENDAR;
import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.DATEPICKER;
import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.SELECT_MANY_MENU;
import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.SELECT_ONE_MENU;
import com.github.adminfaces.addon.util.Constants;
import static com.github.adminfaces.addon.util.Constants.NEW_LINE;
import com.github.adminfaces.addon.util.DependencyUtil;
import com.github.adminfaces.addon.util.TestUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.javaee.cdi.CDIFacet_1_1;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet_2_1;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 *
 * @author rafael-pestano
 */
@RunWith(Arquillian.class)
public class AdminFacesForgeTests {

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private ShellTest shellTest;

    private Project project;

    @Deployment
    @AddonDependencies
    public static AddonArchive getDeployment() {
        return ShrinkWrap.create(AddonArchive.class).addBeansXML().addPackages(true,
            "org.assertj.core").addClass(TestUtils.class)
            .addAsResource("InitDB.java", "InitDB.java")
            .addAsResource("scaffold/custom-global-config.yml", "custom-global-config.yml")
            .addAsResource(AdminFacesForgeTests.class.getResource("/scaffolded-app-with-test-harness.zip"), "scaffolded-app-with-test-harness.zip")
            .addAsResource(AdminFacesForgeTests.class.getResource("/scaffolded-app.zip"), "scaffolded-app.zip")
            .addAsResource(AdminFacesForgeTests.class.getResource("/app-with-adminfaces-setup.zip"), "app-with-adminfaces-setup.zip");
    }

    @Before
    public void setup() throws IllegalArgumentException, NoSuchFieldException, NoSuchFieldException, IllegalAccessException {
        Field globalConfigField = ScaffoldConfigLoader.class.getDeclaredField("globalConfig");
        globalConfigField.setAccessible(true);
        globalConfigField.set(null, null);
        project = projectFactory.createTempProject(Arrays.asList(JavaEE7Facet.class, ServletFacet_3_1.class,
            JPAFacet_2_1.class, FacesFacet_2_0.class, CDIFacet_1_1.class, JavaSourceFacet.class));
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        metadataFacet.setProjectGroupName("com.github.admin.addon");
        metadataFacet.setProjectName("AdminFaces");
        metadataFacet.setProjectVersion("1.0");
        shellTest.getShell().setCurrentResource(project.getRoot());
    }

    @Test
    public void shouldSetupAdminFaces() throws Exception {
        shellTest.execute("jpa-setup --provider Hibernate --container JBOSS_EAP7 --db-type H2 --data-source-name java:jboss/datasources/ExampleDS", 30, TimeUnit.SECONDS);
        shellTest.clearScreen();
        Result result = shellTest.execute("adminfaces-setup", 60, TimeUnit.SECONDS);

        if (result instanceof Failed) {
            ((Failed) result).getException().printStackTrace();
        }
        assertThat(result).isNotNull().isNotInstanceOf(Failed.class);

        List<Result> results = ((CompositeResult) result).getResults();

        assertThat(results).isNotEmpty();

        assertThat(results.get(0)).extracting("message").contains("AdminFaces setup completed successfully!");

        project = projectFactory.findProject(project.getRoot());
        assertThat(project.hasFacet(AdminFacesFacet.class)).isTrue();
        DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);

        assertThat(dependencyFacet.hasDirectDependency(
            DependencyBuilder.create().setCoordinate(DependencyUtil.ADMIN_TEMPLATE_COORDINATE))).isTrue();

        Resource<?> projectRoot = project.getRoot();
        File adminConfig = new File(
            projectRoot.getFullyQualifiedName() + "/src/main/resources/admin-config.properties");
        assertThat(adminConfig).exists().hasContent("admin.renderControlSidebar=true" + NEW_LINE
            + "admin.controlSidebar.showOnMobile=true" + NEW_LINE + "admin.ignoredResources=rest");
        assertThat(new File(projectRoot.getFullyQualifiedName() + "/src/main/resources/messages.properties")).exists();
        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        assertThat(new File(web.getWebResource("index.xhtml").getFullyQualifiedName())).exists();
        assertThat(new File(web.getWebResource("login.xhtml").getFullyQualifiedName())).exists();
        assertThat(new File(web.getWebResource("WEB-INF/faces-config.xml").getFullyQualifiedName())).exists();
        assertThat(new File(web.getWebResource("WEB-INF/web.xml").getFullyQualifiedName())).exists();
        assertThat(new File(web.getWebResource("WEB-INF/beans.xml").getFullyQualifiedName())).exists();
        assertThat(new File(web.getWebResource("includes/menu.xhtml").getFullyQualifiedName())).exists();
        assertThat(new File(web.getWebResource("includes/menubar.xhtml").getFullyQualifiedName())).exists();
        assertThat(new File(web.getWebResource("includes/top-bar.xhtml").getFullyQualifiedName())).exists();

        File template = new File(web.getWebResource("WEB-INF/templates/template.xhtml").getFullyQualifiedName());
        assertThat(template).exists();
        assertThat(contentOf(template)).contains("        <title>Admin Faces</title>")
            .contains("    <ui:define name=\"logo\">" + NEW_LINE + "        Admin Faces" + NEW_LINE
                + "    </ui:define>")
            .contains("    <ui:define name=\"logo-mini\">" + NEW_LINE + "         Adm" + NEW_LINE
                + "    </ui:define>");
        assertThat(new File(web.getWebResource("WEB-INF/templates/template-top.xhtml").getFullyQualifiedName()))
            .exists();

        assertThat(project.getFacet(AdminFacesFacet.class).isInstalled()).isTrue();
        
        DirectoryResource root = project.getRoot().reify(DirectoryResource.class);
        assertThat(root.getChild("Dockerfile").exists());
        File dockerfile = new File(root.getChild("Dockerfile").getFullyQualifiedName());
        assertThat(contentOf(dockerfile)).contains("FROM rmpestano/wildfly:15.0.1")
           .contains("COPY ./target/AdminFaces.war ${DEPLOYMENT_DIR}");
        
        DirectoryResource dockerDir = root.getChildDirectory("docker");
        assertThat(dockerDir.exists()).isTrue();
        
        File dockerRunFile = new File(dockerDir.getFullyQualifiedName()+"/run.sh");
        assertThat(contentOf(dockerRunFile)).contains("docker run -it --rm --name AdminFaces -p 8080:8080 admin/AdminFaces");
        
        File dockerBuildFile = new File(dockerDir.getFullyQualifiedName()+"/build.sh");
        assertThat(contentOf(dockerBuildFile)).contains("docker build -t admin/AdminFaces ../");
        
    }

    @Test
    public void shouldScaffoldFromEntities() throws Exception {
        TestUtils.unzip(getClass().getResourceAsStream("/app-with-adminfaces-setup.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
        project = projectFactory.findProject(project.getRoot());
        generateEntities();
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        IOUtils.copy(getClass().getResourceAsStream("/InitDB.java"),
            new FileOutputStream(new File(sourceFacet.getSourceDirectory().getFullyQualifiedName() + "/com/github/admin/addon/infra/InitDB.java")));
        shellTest.clearScreen();
        Result result = shellTest.execute("scaffold-setup --provider AdminFaces", 1, TimeUnit.MINUTES);
        if (result instanceof Failed) {
            Failed failedResult = (Failed) result;
            failedResult.getException().printStackTrace();
            Assert.fail(failedResult.getMessage());
        }
        assertThat(result).isInstanceOf(CompositeResult.class).extracting("message")
            .contains("***SUCCESS*** Scaffold was setup successfully.");

        String entityPackageName = sourceFacet.getBasePackage() + ".model";

        Result scaffoldGenerate1 = shellTest
            .execute(("scaffold-generate --entities " + entityPackageName + ".*"), 1, TimeUnit.MINUTES);

        if (scaffoldGenerate1 instanceof Failed) {
            ((Failed) scaffoldGenerate1).getException().printStackTrace();
        }
        assertThat(scaffoldGenerate1).isNotInstanceOf(Failed.class);

        Resource<?> src = sourceFacet.getSourceDirectory();
        Resource<?> repository = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.REPOSITORY + "/SpeakerRepository.java");

        assertThat(repository.exists()).isTrue();

        JavaInterfaceSource repositorySource = Roaster.parse(JavaInterfaceSource.class, new File(repository.getFullyQualifiedName()));
        assertThat(repositorySource.hasSyntaxErrors()).isFalse();

        Resource<?> service = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.SERVICE + "/SpeakerService.java");

        assertThat(service.exists()).isTrue();

        JavaClassSource serviceSource = Roaster.parse(JavaClassSource.class, new File(service.getFullyQualifiedName()));
        assertThat(serviceSource.hasSyntaxErrors()).isFalse();

        assertThat(serviceSource.hasMethodSignature("getTalksBySpeakerId", Long.class)).isTrue();

        assertThat(serviceSource.hasMethodSignature("findById", Serializable.class)).isTrue();

        Resource<?> formMB = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.BEAN + "/SpeakerFormMB.java");

        assertThat(formMB.exists()).isTrue();

        JavaClassSource formMBSource = Roaster.parse(JavaClassSource.class, new File(formMB.getFullyQualifiedName()));
        assertThat(formMBSource.hasSyntaxErrors()).isFalse();

        Resource<?> listMB = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.BEAN + "/SpeakerListMB.java");

        assertThat(listMB.exists()).isTrue();

        JavaClassSource listMBSource = Roaster.parse(JavaClassSource.class, new File(listMB.getFullyQualifiedName()));
        assertThat(listMBSource.hasSyntaxErrors()).isFalse();
        assertThat(listMBSource.hasMethodSignature("showTalksDetail", Long.class)).isTrue();
        assertThat(listMBSource.hasMethodSignature("getShowTalksDetailMap")).isTrue();

        repository = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.REPOSITORY + "/TalkRepository.java");

        assertThat(repository.exists()).isTrue();

        repositorySource = Roaster.parse(JavaInterfaceSource.class, new File(repository.getFullyQualifiedName()));
        assertThat(repositorySource.hasSyntaxErrors()).isFalse();

        service = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.SERVICE + "/TalkService.java");

        assertThat(service.exists()).isTrue();

        serviceSource = Roaster.parse(JavaClassSource.class, new File(service.getFullyQualifiedName()));
        assertThat(serviceSource.hasSyntaxErrors()).isFalse();

        assertThat(serviceSource.hasMethodSignature("getSpeakersByFirstname", String.class)).isTrue();

        listMB = src.getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.BEAN + "/TalkListMB.java");

        assertThat(listMB.exists()).isTrue();

        listMBSource = Roaster.parse(JavaClassSource.class, new File(listMB.getFullyQualifiedName()));
        assertThat(listMBSource.hasSyntaxErrors()).isFalse();
        assertThat(listMBSource.hasMethodSignature("completeSpeaker", String.class)).isTrue();

        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        FileResource<?> leftMenu = web.getWebResource(Constants.WebResources.LEFT_MENU);

        File leftMenuFile = new File(leftMenu.getFullyQualifiedName());
        assertThat(leftMenuFile).exists();

        assertThat(contentOf(leftMenuFile))
            .contains("<p:link id=\"menuSpeaker\" outcome=\"/speaker/speaker-list.xhtml\" title=\"Speakers page\"> " + NEW_LINE
                + "                <i class=\"fa fa-circle-o\"></i> " + NEW_LINE
                + "                <span>Speakers</span> " + NEW_LINE
                + "            </p:link>")
            .contains("<p:link id=\"menuTalk\" outcome=\"/talk/talk-list.xhtml\" title=\"Talks page\"> " + NEW_LINE
                + "                <i class=\"fa fa-circle-o\"></i> " + NEW_LINE
                + "                <span>Talks</span> " + NEW_LINE
                + "            </p:link>");

        FileResource<?> topMenu = web.getWebResource(Constants.WebResources.TOP_MENU);
        File topMenuFile = new File(topMenu.getFullyQualifiedName());
        assertThat(topMenuFile).exists();
        assertThat(contentOf(topMenuFile))
            .contains("<li id=\"menuTalk\" class=\"dropdown\"> <a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">Talks <span class=\"caret\"></span> <i class=\"fa fa-circle-o\"></i> </a> ")
            .contains("<li id=\"menuSpeaker\" class=\"dropdown\"> <a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">Speakers <span class=\"caret\"></span> <i class=\"fa fa-circle-o\"></i> </a> ");

        FileResource<?> talkListPage = web.getWebResource("/talk/talk-list.xhtml");

        File talkListPageFile = new File(talkListPage.getFullyQualifiedName());
        assertThat(talkListPageFile).exists();
        assertThat(contentOf(talkListPageFile))
            .contains("<p:column headerText=\"Speaker\" sortBy=\"#{row.speaker}\" priority=\"2\">")
            .contains("#{row.speaker.firstname}");

        FileResource<?> speakerListPage = web.getWebResource("/speaker/speaker-list.xhtml");

        File speakerListPageFile = new File(speakerListPage.getFullyQualifiedName());
        assertThat(speakerListPageFile).exists();
        assertThat(contentOf(speakerListPageFile))
            .contains("<p:dataTable widgetVar=\"speakersTable\" var=\"row\" value=\"#{speakerListMB.list}\" rows=\"10\" rowKey=\"#{row.id}\" lazy=\"true\" paginator=\"true\" sortBy=\"#{row.id}\" reflow=\"true\" ")
            .contains("<p:column headerText=\"Firstname\" sortBy=\"#{row.firstname}\" priority=\"2\"")
            .contains("<h:panelGroup rendered=\"#{not speakerListMB.showTalksDetailMap[row.id]}\" style=\"text-align: center\">")
            .contains("<p:dataList rendered=\"#{speakerListMB.showTalksDetailMap[row.id]}\" emptyMessage=\"#{msg['label.empty-list']}\" value=\"#{speakerListMB.speakerTalks}\" var=\"d\" styleClass=\"no-border\" ");

        MavenFacet maven = project.getFacet(MavenFacet.class);
        boolean buildSuccess = maven.executeMaven(Arrays.asList("clean", "package"));
        assertThat(buildSuccess).isTrue();
    }

    @Test
    public void shouldScaffoldFromEntitiesUsingCustomConfiguration() throws Exception {
        TestUtils.unzip(getClass().getResourceAsStream("/app-with-adminfaces-setup.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
        project = projectFactory.findProject(project.getRoot());
        generateEntities();
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        IOUtils.copy(getClass().getResourceAsStream("/InitDB.java"),
            new FileOutputStream(new File(sourceFacet.getSourceDirectory().getFullyQualifiedName() + "/com/github/admin/addon/infra/InitDB.java")));
        shellTest.clearScreen();
        DirectoryResource resources = project.getFacet(ResourcesFacet.class).getResourceDirectory();
        DirectoryResource scaffoldDir = resources.getOrCreateChildDirectory("scaffold");
        IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("custom-global-config.yml"),
            new FileOutputStream(new File(scaffoldDir.getFullyQualifiedName() + "/global-config.yml")));

        Result result = shellTest.execute("scaffold-setup --provider AdminFaces", 1, TimeUnit.MINUTES);

        if (result instanceof Failed) {
            ((Failed) result).getException().printStackTrace();
        }
        assertThat(result).isInstanceOf(CompositeResult.class).extracting("message")
            .contains("***SUCCESS*** Scaffold was setup successfully.");

        String entityPackageName = sourceFacet.getBasePackage() + ".model";
        Resource<?> src = sourceFacet.getSourceDirectory();

        Result scaffoldGenerate1 = shellTest
            .execute(("scaffold-generate --entities " + entityPackageName + ".*"), 1, TimeUnit.MINUTES);

        if (scaffoldGenerate1 instanceof Failed) {
            ((Failed) scaffoldGenerate1).getException().printStackTrace();
        }
        assertThat(scaffoldGenerate1).isNotInstanceOf(Failed.class);

        Resource<?> service = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.SERVICE + "/SpeakerService.java");

        assertThat(service.exists()).isTrue();

        JavaClassSource serviceSource = Roaster.parse(JavaClassSource.class, new File(service.getFullyQualifiedName()));
        assertThat(serviceSource.hasSyntaxErrors()).isFalse();

        Resource<?> listMB = src
            .getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.BEAN + "/SpeakerListMB.java");

        assertThat(listMB.exists()).isTrue();

        JavaClassSource listMBSource = Roaster.parse(JavaClassSource.class, new File(listMB.getFullyQualifiedName()));
        assertThat(listMBSource.hasSyntaxErrors()).isFalse();

        assertThat(listMBSource.hasMethodSignature("onRowEdit", "org.primefaces.event.RowEditEvent")).isTrue();

        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        FileResource<?> speakerListPage = web.getWebResource("/speaker/speaker-list.xhtml");

        File speakerListPageFile = new File(speakerListPage.getFullyQualifiedName());
        assertThat(speakerListPageFile).exists();
        assertThat(contentOf(speakerListPageFile))
            .contains("editable=\"true\"")
            .doesNotContain("reflow=\"true\"")
            .contains("<p:ajax event=\"rowEdit\" listener=\"#{speakerListMB.onRowEdit}\"")
            .contains("<p:selectManyMenu id=\"talks\" value=\"#{speakerListMB.filter.entity.talks}\"")
            .contains("<h:panelGroup rendered=\"#{not speakerListMB.showTalksDetailMap[row.id]}\" style=\"text-align: center\">")
            .contains("<p:dataList rendered=\"#{speakerListMB.showTalksDetailMap[row.id]}\" emptyMessage=\"#{msg['label.empty-list']}\" value=\"#{speakerListMB.speakerTalks}\" var=\"d\" styleClass=\"no-border\"");

        FileResource<?> talkListPage = web.getWebResource("/talk/talk-list.xhtml");

        File speakertalkListPageFile = new File(talkListPage.getFullyQualifiedName());
        assertThat(speakertalkListPageFile).exists();
        assertThat(contentOf(speakertalkListPageFile))
            .contains("<p:selectOneMenu id=\"room\" value=\"#{talkListMB.filter.entity.room}\" converter=\"entityConverter\"> ")
            .contains("<p:selectOneMenu id=\"speaker\" value=\"#{talkListMB.filter.entity.speaker}\" converter=\"entityConverter\">");

        MavenFacet maven = project.getFacet(MavenFacet.class);
        boolean buildSuccess = maven.executeMaven(Arrays.asList("clean", "package"));
        assertThat(buildSuccess).isTrue();
    }

    @Test
    public void shouldEditGlobalConfigViaScaffoldConfigCommand() throws TimeoutException, IOException {
        TestUtils.unzip(getClass().getResourceAsStream("/scaffolded-app.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
        project = projectFactory.findProject(project.getRoot());
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        Resource<?> scaffoldDir = resourcesFacet.getResourceDirectory().getChild("scaffold");
        FileResource<?> globalScaffoldConfig = scaffoldDir.getChild("global-config.yml").reify(FileResource.class);
        StringBuilder scaffoldConfigCommand = new StringBuilder("adminfaces-scaffold-config --config-file ")
            .append(globalScaffoldConfig.getFullyQualifiedName()).append(" --input-size 30 --to-one-component-type ")
            .append(SELECT_ONE_MENU.name()).append(" --to-many-component-type ").append(SELECT_MANY_MENU.name())
            .append(" --datatable-editable --menu-icon \"fa fa-edit\"");
        Result scaffoldConfigResult = shellTest.execute(scaffoldConfigCommand.toString(), 1, TimeUnit.MINUTES);
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
        TestUtils.unzip(getClass().getResourceAsStream("/scaffolded-app.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
        project = projectFactory.findProject(project.getRoot());
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        Resource<?> scaffoldDir = resourcesFacet.getResourceDirectory().getChild("scaffold");
        FileResource<?> talkScaffoldConfig = scaffoldDir.getChild("Talk.yml").reify(FileResource.class);
        StringBuilder scaffoldConfigCommand = new StringBuilder("adminfaces-scaffold-config --config-file ")
            .append(talkScaffoldConfig.getFullyQualifiedName()).append(" --choice-field-to-change date --type ").append(DATEPICKER.name())
            .append(" --datatable-editable --menu-icon \"fa fa-edit\"");
        Result scaffoldConfigResult = shellTest.execute(scaffoldConfigCommand.toString(), 1, TimeUnit.MINUTES);
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
            .contains("!!com.github.adminfaces.addon.scaffold.model.EntityConfig" + NEW_LINE
                + "datatableEditable: true" + NEW_LINE
                + "datatableReflow: true" + NEW_LINE
                + "displayField: title" + NEW_LINE
                + "fields:" + NEW_LINE
                + "- hidden: false" + NEW_LINE
                + "  length: 100" + NEW_LINE
                + "  name: id" + NEW_LINE
                + "  required: true" + NEW_LINE
                + "  type: INPUT_NUMBER" + NEW_LINE
                + "- hidden: false" + NEW_LINE
                + "  length: 100" + NEW_LINE
                + "  name: version" + NEW_LINE
                + "  required: false" + NEW_LINE
                + "  type: INPUT_NUMBER" + NEW_LINE
                + "- hidden: false" + NEW_LINE
                + "  length: 100" + NEW_LINE
                + "  name: title" + NEW_LINE
                + "  required: true" + NEW_LINE
                + "  type: INPUT_TEXT" + NEW_LINE
                + "- hidden: false" + NEW_LINE
                + "  length: 2000" + NEW_LINE
                + "  name: description" + NEW_LINE
                + "  required: true" + NEW_LINE
                + "  type: TEXT_AREA" + NEW_LINE
                + "- hidden: false" + NEW_LINE
                + "  length: 100" + NEW_LINE
                + "  name: date" + NEW_LINE
                + "  required: true" + NEW_LINE
                + "  type: DATEPICKER" + NEW_LINE
                + "- hidden: false" + NEW_LINE
                + "  length: 100" + NEW_LINE
                + "  name: speaker" + NEW_LINE
                + "  required: true" + NEW_LINE
                + "  type: AUTOCOMPLETE" + NEW_LINE
                + "- hidden: false" + NEW_LINE
                + "  length: 100" + NEW_LINE
                + "  name: room" + NEW_LINE
                + "  required: true" + NEW_LINE
                + "  type: AUTOCOMPLETE" + NEW_LINE
                + "menuIcon: fa fa-circle-o");
    }
    
    @Test
    public void shouldSetUpAdminFacesTestHarness() throws TimeoutException, IOException {
        TestUtils.unzip(getClass().getResourceAsStream("/app-with-adminfaces-setup.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
        project = projectFactory.findProject(project.getRoot());
        Result testHarnessSetupResult = shellTest
            .execute("adminfaces-test-harness-setup", 2, TimeUnit.MINUTES);
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
    
    @Test
    public void shouldCreateServiceTests() throws IOException, TimeoutException, InterruptedException {
        project = projectFactory.createTempProject();
        TestUtils.unzip(getClass().getResourceAsStream("/scaffolded-app-with-test-harness.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
        project = projectFactory.findProject(project.getRoot());
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        String servicePackageName = sourceFacet.getBasePackage() + ".service";
        Result newServicetestResult = shellTest
            .execute("adminfaces-new-service-test --target-services " + servicePackageName + ".*", 1, TimeUnit.MINUTES);
        if (newServicetestResult instanceof Failed) {
            Failed failedResult = (Failed) newServicetestResult;
            failedResult.getException().printStackTrace();
            Assert.fail(failedResult.getMessage());
        }
        List<Result> results = ((CompositeResult) newServicetestResult).getResults();
        assertThat(results).hasSize(7);
        assertThat(results).extracting("message")
            .contains("Added /src/test/resources/datasets/room.yml")
            .contains("Service test(s) created successfully!")
            .contains("Added /src/test/resources/datasets/talk.yml")
            .contains("Added /src/test/resources/datasets/speaker.yml")
            .contains("Added /src/test/java/com/github/admin/addon/service/RoomServiceIt.java")
            .contains("Added /src/test/java/com/github/admin/addon/service/TalkServiceIt.java")
            .contains("Added /src/test/java/com/github/admin/addon/service/SpeakerServiceIt.java");
        
        ResourcesFacet resourcesFacet = project.getFacet(ResourcesFacet.class);
        FileResource<?> testPersistenceXML = resourcesFacet.getTestResourceDirectory().getChild("META-INF").getChild("persistence.xml").reify(FileResource.class);
        File persistenceXmlFile = new File(testPersistenceXML.getFullyQualifiedName());
        assertThat(contentOf(persistenceXmlFile))
            .contains("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" +
"<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"2.0\" xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_2_0.xsd\">\n" +
"  <persistence-unit name=\"testDB\" transaction-type=\"RESOURCE_LOCAL\">\n" +
"    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>\n" +
"    <class>com.github.admin.addon.model.Room</class>\n" +
"    <class>com.github.admin.addon.model.Speaker</class>\n" +
"    <class>com.github.admin.addon.model.Talk</class>\n" +
"    <properties>\n" +
"      <property name=\"hibernate.dialect\" value=\"org.hibernate.dialect.HSQLDialect\"/>\n" +
"      <property name=\"javax.persistence.jdbc.driver\" value=\"org.hsqldb.jdbcDriver\"/>\n" +
"      <property name=\"javax.persistence.jdbc.url\" value=\"jdbc:hsqldb:mem:test;DB_CLOSE_DELAY=-1\"/>\n" +
"      <property name=\"javax.persistence.jdbc.user\" value=\"sa\"/>\n" +
"      <property name=\"javax.persistence.jdbc.password\" value=\"\"/>\n" +
"      <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>\n" +
"      <property name=\"hibernate.show_sql\" value=\"true\"/>\n" +
"    </properties>\n" +
"  </persistence-unit>\n" +
"</persistence>");
        
        MavenFacet maven = project.getFacet(MavenFacet.class);
        boolean buildSuccess = maven.executeMaven(Arrays.asList("clean", "package", "-Pit-tests"));
        assertThat(buildSuccess).isTrue();
    }

    @After
    public void clear() throws Exception {
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

        shellTest.execute("jpa-new-field --named talks --type com.github.admin.addon.model.Talk --relationship-type One-to-Many --inverse-field-name speaker", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named address --entity --type com.github.admin.addon.model.Address --relationship-type Embedded", 10, TimeUnit.SECONDS);

        shellTest.execute("constraint-add --on-property firstname --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property surname --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property bio --constraint Size --max 2000", 10, TimeUnit.SECONDS);

        shellTest.execute("cd ../Talk.java", 15, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named room --type com.github.admin.addon.model.Room --relationship-type Many-to-One --inverse-field-name talks", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property speaker --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property room --constraint NotNull", 10, TimeUnit.SECONDS);
    }

}
