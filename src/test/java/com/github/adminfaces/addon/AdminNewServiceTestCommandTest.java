package com.github.adminfaces.addon;

import com.github.adminfaces.addon.util.TestUtils;
import java.io.File;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
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
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static org.assertj.core.api.AssertionsForClassTypes.contentOf;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.FileResource;
import org.junit.Assert;

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
            "org.assertj.core").addClass(TestUtils.class)
            .addAsResource(AdminNewServiceTestCommandTest.class.getResource("/scaffolded-app-with-test-harness.zip"), "scaffolded-app-with-test-harness.zip");
    }

    @Before
    public void setUp() throws IOException, TimeoutException {
        project = projectFactory.createTempProject();
        TestUtils.unzip(getClass().getResourceAsStream("/scaffolded-app-with-test-harness.zip"), project.getRoot().getFullyQualifiedName());
        shellTest.getShell().setCurrentResource(project.getRoot());
        shellTest.clearScreen();
    }

    @Test
    public void shouldCreateServiceTests() throws IOException, TimeoutException, InterruptedException {
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
        FileResource testPersistenceXML = resourcesFacet.getTestResourceDirectory().getChild("META-INF").getChild("persistence.xml").reify(FileResource.class);
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
    }

    @After
    public void tearDown() throws Exception {
        shellTest.close();
    }
}
