package com.github.admin.addon;

import com.github.adminfaces.addon.scaffold.config.ScaffoldConfigLoader;
import static com.github.adminfaces.addon.util.Constants.NEW_LINE;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.servlet.ServletFacet_3_1;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.shell.test.ShellTest;
import org.jboss.forge.addon.ui.result.CompositeResult;
import org.jboss.forge.addon.ui.result.Failed;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.arquillian.AddonDependencies;
import org.jboss.forge.arquillian.archive.AddonArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.adminfaces.addon.util.Constants;
import java.io.File;
import java.io.FileOutputStream;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.io.IOUtils;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import org.jboss.forge.addon.javaee.jpa.JPAFacet_2_1;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

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
        return ShrinkWrap.create(AddonArchive.class).addBeansXML().addClass(TestUtil.class)
            .addPackages(true, "org.assertj.core")
            .addAsResource("scaffold/custom-global-config.yml", "custom-global-config.yml")
            .addAsResource("InitDB.java", "InitDB.java");
    }

    @Before
    public void setUp() throws IOException, TimeoutException, NoSuchFieldException, IllegalArgumentException, IllegalArgumentException, IllegalAccessException, IllegalAccessException {
        Field globalConfigField = ScaffoldConfigLoader.class.getDeclaredField("globalConfig");
        globalConfigField.setAccessible(true);
        globalConfigField.set(null, null);
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
        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        IOUtils.copy(getClass().getResourceAsStream("/InitDB.java"),
            new FileOutputStream(new File(sourceFacet.getSourceDirectory().getFullyQualifiedName() + "/com/github/admin/addon/infra/InitDB.java")));
    }

    @Test
    public void shouldScaffoldFromEntities() throws Exception {
        Result result = shellTest.execute("scaffold-setup --provider AdminFaces", 10, TimeUnit.MINUTES);
        if (result instanceof Failed) {
            ((Failed) result).getException().printStackTrace();
        }
        assertThat(result).isInstanceOf(CompositeResult.class).extracting("message")
            .contains("***SUCCESS*** Scaffold was setup successfully.");

        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        String entityPackageName = sourceFacet.getBasePackage() + ".model";
        Resource<?> src = sourceFacet.getSourceDirectory();

        Result scaffoldGenerate1 = shellTest
            .execute(("scaffold-generate --entities " + entityPackageName + ".*"), 10, TimeUnit.MINUTES);

        if (scaffoldGenerate1 instanceof Failed) {
            ((Failed) scaffoldGenerate1).getException().printStackTrace();
        }
        assertThat(scaffoldGenerate1).isNotInstanceOf(Failed.class);

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

        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
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
        shellTest.execute("jpa-new-field --named talks --type com.github.admin.addon.model.Talk --relationship-type One-to-Many", 10, TimeUnit.SECONDS);

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
        //shellTest.execute("jpa-new-field --named speaker --type com.github.admin.addon.model.Speaker --relationship-type Many-to-One", 10, TimeUnit.SECONDS); 
        shellTest.execute("jpa-new-field --named room --type com.github.admin.addon.model.Room --relationship-type Many-to-One", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property speaker --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property room --constraint NotNull", 10, TimeUnit.SECONDS);
    }

}
