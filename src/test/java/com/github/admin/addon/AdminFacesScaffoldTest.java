package com.github.admin.addon;

import static com.github.adminfaces.addon.util.Constants.NEW_LINE;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.javaee.faces.FacesFacet_2_0;
import org.jboss.forge.addon.javaee.facets.JavaEE7Facet;
import org.jboss.forge.addon.javaee.jpa.JPAFacet;
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

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import static org.assertj.core.api.Assertions.contentOf;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

/**
 * Test class for
 * {@link com.github.adminfaces.addon.scaffold.AdminFacesScaffoldProvider}
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
    public void shouldScaffoldFromEntities() throws Exception {
        shellTest.execute("jpa-new-entity --named Talk", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named title", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named description --length 2000", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named room", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named date --type java.util.Calendar --temporal-type DATE", 10, TimeUnit.SECONDS);
       
        shellTest.execute("constraint-add --on-property title --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property room --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property description --constraint Size --max 2000", 10, TimeUnit.SECONDS);
        
        shellTest.execute("jpa-new-entity --named Speaker", 15, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named firstname", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named surname", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named bio --length 2000", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named twitter", 10, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named talks --type com.github.admin.addon.model.Talk --relationship-type One-to-Many", 10, TimeUnit.SECONDS);
        
        shellTest.execute("constraint-add --on-property firstname --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property surname --constraint NotNull", 10, TimeUnit.SECONDS);
        shellTest.execute("constraint-add --on-property bio --constraint Size --max 2000", 10, TimeUnit.SECONDS);
        
        shellTest.execute("cd ../Talk.java",15, TimeUnit.SECONDS);
        shellTest.execute("jpa-new-field --named speaker --type com.github.admin.addon.model.Speaker --relationship-type Many-to-One", 10, TimeUnit.SECONDS); 
        
        Result result = shellTest.execute("scaffold-setup --provider AdminFaces", 10, TimeUnit.MINUTES);
        assertThat(result).isInstanceOf(CompositeResult.class).extracting("message")
            .contains("***SUCCESS*** Scaffold was setup successfully.");

        JavaSourceFacet sourceFacet = project.getFacet(JavaSourceFacet.class);
        String entityPackageName = sourceFacet.getBasePackage() + ".model";
        Resource<?> src = sourceFacet.getSourceDirectory();
        
        Resource<?> speakerResource = src.getChild(sourceFacet.getBasePackage().replaceAll("\\.", "/"))
            .getChild(Constants.Packages.MODEL + "/Speaker.java");
        JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);
        JavaClassSource speakerSource = Roaster.parse(JavaClassSource.class, new File(speakerResource.getFullyQualifiedName()));
        speakerSource.getField("twitter").getJavaDoc().addTagValue("@ui-autocomplete", "");
        javaSource.saveJavaSource(speakerSource);
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
        
        assertThat(serviceSource.hasMethodSignature("getTwitters", String.class)).isTrue();

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
        
        WebResourcesFacet web = project.getFacet(WebResourcesFacet.class);
        FileResource<?> leftMenu = web.getWebResource(Constants.WebResources.LEFT_MENU);
        
        File leftMenuFile = new File(leftMenu.getFullyQualifiedName());
        assertThat(leftMenuFile).exists();

        assertThat(contentOf(leftMenuFile))
            .contains("<p:link id=\"menuSpeaker\" outcome=\"/speaker/speaker-list.xhtml\" title=\"Speakers page\"> "+NEW_LINE +
"                <i class=\"fa fa-circle-o\"></i> "+NEW_LINE +
"                <span>Speakers</span> "+NEW_LINE +
"            </p:link>")
            .contains("<p:link id=\"menuTalk\" outcome=\"/talk/talk-list.xhtml\" title=\"Talks page\"> "+NEW_LINE +
"                <i class=\"fa fa-circle-o\"></i> "+NEW_LINE +
"                <span>Talks</span> "+NEW_LINE +
"            </p:link>");
        
        FileResource<?> topMenu = web.getWebResource(Constants.WebResources.TOP_MENU);
        File topMenuFile = new File(topMenu.getFullyQualifiedName());
        assertThat(topMenuFile).exists();
        assertThat(contentOf(topMenuFile))
            .contains("<li id=\"menuTalk\" class=\"dropdown\"> <a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">Talks <span class=\"caret\"></span> <i class=\"fa fa-circle-o\"></i> </a> ")
            .contains("<li id=\"menuSpeaker\" class=\"dropdown\"> <a href=\"#\" class=\"dropdown-toggle\" data-toggle=\"dropdown\">Speakers <span class=\"caret\"></span> <i class=\"fa fa-circle-o\"></i> </a> ");
        
        Result projectBuildResult = shellTest
            .execute("build ", 1, TimeUnit.MINUTES);
        if (projectBuildResult instanceof Failed) {
        	System.out.println("Message: "+((Failed) projectBuildResult).getException().getMessage());
        	System.out.println("LocalizedMessage: "+((Failed) projectBuildResult).getException().getLocalizedMessage());
            ((Failed) projectBuildResult).getException().printStackTrace();
        }
        assertThat(projectBuildResult).isNotInstanceOf(Failed.class);
    }

    /*
	 * @Test public void shouldCreateOneErrorPageForEachErrorCode() throws Exception
	 * { shellTest.execute("servlet-setup --servlet-version 3.1", 10,
	 * TimeUnit.SECONDS); shellTest.execute("jpa-setup", 10, TimeUnit.SECONDS);
	 * shellTest.execute("jpa-new-entity --named Customer", 10, TimeUnit.SECONDS);
	 * shellTest.execute("jpa-new-field --named firstName", 10, TimeUnit.SECONDS);
	 * shellTest.execute("jpa-new-entity --named Publisher", 10, TimeUnit.SECONDS);
	 * shellTest.execute("jpa-new-field --named firstName", 10, TimeUnit.SECONDS);
	 * Result result = shellTest.execute("scaffold-setup --provider Faces", 10,
	 * TimeUnit.SECONDS); Assert.assertThat(result, not(instanceOf(Failed.class)));
	 * Project project =
	 * projectFactory.findProject(shellTest.getShell().getCurrentResource());
	 * Assert.assertTrue(project.hasFacet(ServletFacet_3_1.class)); ServletFacet_3_1
	 * servletFacet = project.getFacet(ServletFacet_3_1.class);
	 * Assert.assertNotNull(servletFacet.getConfig());
	 * 
	 * String entityPackageName =
	 * project.getFacet(JavaSourceFacet.class).getBasePackage() + ".model"; Result
	 * scaffoldGenerate1 = shellTest
	 * .execute(("scaffold-generate --web-root /admin --targets " +
	 * entityPackageName + ".Customer"), 10, TimeUnit.SECONDS);
	 * Assert.assertThat(scaffoldGenerate1, not(instanceOf(Failed.class)));
	 * 
	 * Assert.assertEquals(2, servletFacet.getConfig().getAllErrorPage().size());
	 * 
	 * Result scaffoldGenerate2 = shellTest
	 * .execute(("scaffold-generate --web-root /admin --targets " +
	 * entityPackageName + ".Publisher"), 10, TimeUnit.SECONDS);
	 * Assert.assertThat(scaffoldGenerate2, not(instanceOf(Failed.class)));
	 * Assert.assertEquals(2, servletFacet.getConfig().getAllErrorPage().size()); }
	 * 
	 * @Test public void shouldScaffoldEntity() throws Exception {
	 * Assert.assertThat(shellTest.execute("javaee-setup --java-ee-version 7", 10,
	 * TimeUnit.SECONDS), not(instanceOf(Failed.class)));
	 * Assert.assertThat(shellTest.execute("jpa-setup", 10, TimeUnit.SECONDS),
	 * not(instanceOf(Failed.class)));
	 * Assert.assertThat(shellTest.execute("jpa-new-entity --named Customer", 10,
	 * TimeUnit.SECONDS), not(instanceOf(Failed.class)));
	 * Assert.assertThat(shellTest.execute("jpa-new-field --named firstName", 10,
	 * TimeUnit.SECONDS), not(instanceOf(Failed.class))); Result result =
	 * shellTest.execute("scaffold-setup --provider Faces", 10, TimeUnit.SECONDS);
	 * Assert.assertThat(result, not(instanceOf(Failed.class)));
	 * 
	 * Project project =
	 * projectFactory.findProject(shellTest.getShell().getCurrentResource()); String
	 * entityPackageName = project.getFacet(JavaSourceFacet.class).getBasePackage()
	 * + ".model"; result = shellTest.execute(
	 * "scaffold-generate --provider Faces --targets " + entityPackageName +
	 * ".Customer", 10, TimeUnit.SECONDS); Assert.assertThat(result,
	 * not(instanceOf(Failed.class))); }
     */

 /*@After
	public void tearDown() throws Exception {
		if (project != null) {
			project.getRoot().delete(true);
		}
		shellTest.close();
	}*/
}
