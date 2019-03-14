/*
 * The MIT License
 *
 * Copyright 2019 rmpestano.
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
package com.github.adminfaces.addon.ui;

import com.github.adminfaces.addon.facet.AdminFacesFacet;
import com.github.adminfaces.addon.facet.AdminFacesTestHarnessFacet;
import com.github.adminfaces.addon.util.AdminScaffoldUtils;
import static com.github.adminfaces.addon.util.AdminScaffoldUtils.LOG;
import com.github.adminfaces.addon.util.DependencyUtil;
import static com.github.adminfaces.addon.util.DependencyUtil.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.logging.Level;
import javax.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.result.Result;

/**
 *
 * @author rmpestano
 */
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.maven.projects.MavenFacet;
import org.jboss.forge.addon.maven.resources.MavenModelResource;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.parser.xml.Node;
import org.jboss.forge.parser.xml.XMLParser;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaSource;

@FacetConstraint(AdminFacesFacet.class)
public class AdminTestSetupCommand extends AbstractProjectCommand {

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    private DependencyUtil dependencyUtil;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass()).name("AdminFaces: Test harness setup").category(Categories.create("AdminFaces"))
            .description("Setup test dependencies and resources for AdminFaces projects.");
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final Project project = getSelectedProject(context) != null ? getSelectedProject(context)
            : getSelectedProject(context.getUIContext());

        boolean execute = true;
        if (project.hasFacet(AdminFacesTestHarnessFacet.class) && project.getFacet(AdminFacesTestHarnessFacet.class).isInstalled()) {
            execute = context.getPrompt().promptBoolean("AdminFaces test harness is already installed, override it?");
        }

        if (!execute) {
            return Results.success();
        }
        addAdminFacesTestDependencies(project);
        addAdminFacesTestHarnessResources(project);
        addTestEntityManagerProducer(project);
        addMavenTestsProfile(project);
        return Results.success("AdminFaces test harness setup finished successfully!");
    }

    protected void addAdminFacesTestHarnessResources(Project project) {
        AdminScaffoldUtils.setupAdminPersistence(project, dependencyUtil, facetFactory);
        DirectoryResource testResources = project.getFacet(ResourcesFacet.class).getTestResourceDirectory();
        testResources.getOrCreateChildDirectory("datasets");
        DirectoryResource testMetaInf = testResources.getOrCreateChildDirectory("META-INF");
        if (!testMetaInf.getChild("beans.xml").exists()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/META-INF/test-beans.xml")) {
                IOUtils.copy(is, new FileOutputStream(
                    new File(testMetaInf.getFullyQualifiedName() + "/beans.xml")));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not add 'beans.xml'.", e);
            }
        }

        if (!testMetaInf.getChild("apache-deltaspike.properties").exists()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/META-INF/test-apache-deltaspike.properties")) {
                IOUtils.copy(is, new FileOutputStream(
                    new File(testMetaInf.getFullyQualifiedName() + "/apache-deltaspike.properties")));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not add 'apache-deltaspike.properties'.", e);
            }
        }

        if (!testMetaInf.getChild("persistence.xml").exists()) {
            try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("/META-INF/test-persistence.xml")) {
                IOUtils.copy(is, new FileOutputStream(
                    new File(testMetaInf.getFullyQualifiedName() + "/persistence.xml")));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Could not add 'persistence.xml'.", e);
            }
        }

        //add beans.xml to meta-inf in sources (nneded by deltaspike test control)
        DirectoryResource metaInf = project.getFacet(ResourcesFacet.class).getResourceDirectory().getChildDirectory("META-INF");
        if (!metaInf.getChild("beans.xml").exists()) {
            try (InputStream is = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<beans xmlns=\"http://java.sun.com/xml/ns/javaee\"\n"
                + "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "       xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd\">\n"
                + "</beans>").getBytes())) {
                IOUtils.copy(is, new FileOutputStream(
                    new File(metaInf.getFullyQualifiedName() + "/beans.xml")));
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Could not add 'beans.xml'.", e);
            }
        }
    }

    protected void addAdminFacesTestDependencies(Project project) {
        DependencyFacet dependencyFacet = project.getFacet(DependencyFacet.class);
        DependencyBuilder junit = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(JUNIT4_COORDINATE.getArtifactId())
            .setGroupId(JUNIT4_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(junit)) {
            junit.setCoordinate(dependencyUtil.getLatestVersion(JUNIT4_COORDINATE));
            dependencyUtil.installDependency(dependencyFacet, junit);
        }
        DependencyBuilder assertJ = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(ASSERTJ_COORDINATE.getArtifactId())
            .setGroupId(ASSERTJ_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(assertJ)) {
            assertJ.setVersion(ASSERTJ_COORDINATE.getVersion());
            dependencyUtil.installDependency(dependencyFacet, assertJ);
        }
        DependencyBuilder hsqldb = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(HSQLSDB_COORDINATE.getArtifactId())
            .setGroupId(HSQLSDB_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(hsqldb)) {
            hsqldb.setVersion(HSQLSDB_COORDINATE.getVersion());
            dependencyUtil.installDependency(dependencyFacet, hsqldb);
        }
        DependencyBuilder dbRider = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(DBRIDER_COORDINATE.getArtifactId())
            .setGroupId(DBRIDER_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(dbRider)) {
            dbRider.setCoordinate(dependencyUtil.getLatestVersion(DBRIDER_COORDINATE));
            dependencyUtil.installDependency(dependencyFacet, dbRider);
        }
        DependencyBuilder deltaspikeCoreAPi = DependencyBuilder.create()
            .setScopeType("compile")
            .setArtifactId(DELTASPIKE_CORE_API_COORDINATE.getArtifactId())
            .setGroupId(DELTASPIKE_CORE_API_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(deltaspikeCoreAPi)) {
            deltaspikeCoreAPi.setVersion(DELTASPIKE_CORE_API_COORDINATE.getVersion());
            dependencyUtil.installDependency(dependencyFacet, deltaspikeCoreAPi);
        }
        DependencyBuilder deltaSpikeTestControl = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(DELTASPIKE_TESTCONTROL_COORDINATE.getArtifactId())
            .setGroupId(DELTASPIKE_TESTCONTROL_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(deltaSpikeTestControl)) {
            deltaSpikeTestControl.setVersion(DELTASPIKE_TESTCONTROL_COORDINATE.getVersion());
            dependencyUtil.installDependency(dependencyFacet, deltaSpikeTestControl);
        }
        DependencyBuilder deltaSpikeCDIControl = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(DELTASPIKE_CDICONTROL_COORDINATE.getArtifactId())
            .setGroupId(DELTASPIKE_CDICONTROL_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(deltaSpikeCDIControl)) {
            deltaSpikeCDIControl.setVersion(DELTASPIKE_CDICONTROL_COORDINATE.getVersion());
            dependencyUtil.installDependency(dependencyFacet, deltaSpikeCDIControl);
        }
        DependencyBuilder owb = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(OPENWEBBEANS_COORDINATE.getArtifactId())
            .setGroupId(OPENWEBBEANS_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(owb)) {
            owb.setVersion(OPENWEBBEANS_COORDINATE.getVersion());
            dependencyUtil.installDependency(dependencyFacet, owb);
        }
        DependencyBuilder hibernateCore = DependencyBuilder.create()
            .setScopeType("provided")
            .setArtifactId(HIBERNATE_CORE_COORDINATE.getArtifactId())
            .setGroupId(HIBERNATE_CORE_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(hibernateCore)) {
            hibernateCore.setVersion(HIBERNATE_CORE_COORDINATE.getVersion());
            dependencyUtil.installDependency(dependencyFacet, hibernateCore);
        }
        //enforce same version of hibernate-core and hibernate-entitymanager
        Dependency hibernateCoreInstalled = dependencyFacet.getDirectDependency(hibernateCore);
        DependencyBuilder hibernateEntityManager = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId(HIBERNATE_ENTITYMANAGER_COORDINATE.getArtifactId())
            .setGroupId(HIBERNATE_ENTITYMANAGER_COORDINATE.getGroupId());
        if (!dependencyFacet.hasDirectDependency(hibernateEntityManager)) {
            hibernateEntityManager.setVersion(hibernateCoreInstalled.getCoordinate().getVersion());
            dependencyUtil.installDependency(dependencyFacet, hibernateEntityManager);
        } else if (!dependencyFacet.getDirectDependency(hibernateEntityManager).getCoordinate().getVersion().equals(hibernateCoreInstalled.getCoordinate().getVersion())) {
            hibernateEntityManager.setVersion(hibernateCoreInstalled.getCoordinate().getVersion());
            dependencyUtil.reInstallDependency(dependencyFacet, hibernateEntityManager);
        }
        DependencyBuilder servletApi = DependencyBuilder.create()
            .setScopeType("test")
            .setArtifactId("javax.servlet-api")
            .setGroupId("javax.servlet");
        if (!dependencyFacet.hasDirectDependency(servletApi)) {
            servletApi.setVersion("3.1.0");
            dependencyUtil.installDependency(dependencyFacet, servletApi);
        }
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    private void addTestEntityManagerProducer(Project project) {
        MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
        JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);
        try (InputStream emProducerStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("/infra/persistence/TestEntityManagerProducer.java")) {
            JavaSource<?> entityManagerProducer = (JavaSource<?>) Roaster.parse(emProducerStream);
            entityManagerProducer.setPackage(metadataFacet.getProjectGroupName() + ".infra");
            javaSource.saveTestJavaSource(entityManagerProducer);
            FileUtils.copyInputStreamToFile(emProducerStream, new File(javaSource.getTestSourceDirectory().getFullyQualifiedName()
                + "/" + entityManagerProducer.getPackage().replaceAll("\\.", "/")));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not add 'EntityManagerProducer'.", e);
        }
    }

    private void addMavenTestsProfile(Project project) {
        MavenFacet m2 = project.getFacet(MavenFacet.class);
        MavenModelResource m2Model = m2.getModelResource();
        Node node = XMLParser.parse(m2Model.getResourceInputStream());
        Node profiles = node.getOrCreate("profiles");
        Optional<Node> itTestsProfile = profiles.get("profile")
            .stream().filter(p -> p.getName().equals("id") && p.getText().equalsIgnoreCase("it-tests"))
            .findFirst();

        if (!itTestsProfile.isPresent()) {
            Node itTests = profiles.createChild("profile");
            itTests.createChild("id").text("it-tests");
            Node sureFirePlugin = itTests.createChild("build")
                .createChild("plugins").createChild("plugin");
            sureFirePlugin.createChild("artifactId")
                .text("maven-surefire-plugin");
            sureFirePlugin.createChild("version")
                .text("2.22.1");
            sureFirePlugin.createChild("configuration")
                .createChild("includes").text("**/*It.java");
            m2Model.setContents(XMLParser.toXMLInputStream(node));
        }
    }

}
