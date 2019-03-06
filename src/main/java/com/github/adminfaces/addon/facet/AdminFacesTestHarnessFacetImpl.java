package com.github.adminfaces.addon.facet;

import com.github.adminfaces.addon.util.AdminScaffoldUtils;
import static com.github.adminfaces.addon.util.AdminScaffoldUtils.LOG;
import com.github.adminfaces.addon.util.DependencyUtil;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.WebResourcesFacet;

import javax.inject.Inject;

import static com.github.adminfaces.addon.util.DependencyUtil.*;
import static com.github.adminfaces.addon.util.Constants.WebResources.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import org.apache.commons.io.IOUtils;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.Resource;

/**
 * The implementation of the {@link AdminFacesFacet}
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
public class AdminFacesTestHarnessFacetImpl extends AbstractFacet<Project> implements AdminFacesTestHarnessFacet {

    @Inject
    private DependencyUtil dependencyUtil;

    @Inject
    private FacetFactory facetFactory;

    @Override
    public boolean install() {
        addAdminFacesTestHarnessDependencies();
        return isInstalled();
    }

    private void addAdminFacesTestHarnessDependencies() {
        AdminScaffoldUtils.setupAdminPersistece(getFaceted(), dependencyUtil, facetFactory);
        addTestResources();
    }

    @Override
    public boolean isInstalled() {
        DependencyFacet facet = getFaceted().getFacet(DependencyFacet.class);
        return hasAdminPersistenceDependency(facet)
            && hasHsqldbDependency(facet) && hasJUnit4Dependency(facet)
            && hasDBRiderDependency(facet) && hasTestResources();
    }

    private static boolean hasAdminPersistenceDependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(ADMIN_PERSISTENCE_COORDINATE.getArtifactId())
            .setGroupId(ADMIN_PERSISTENCE_COORDINATE.getGroupId()));
    }

    private static boolean hasHsqldbDependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(HSQLSDB_COORDINATE.getArtifactId())
            .setGroupId(HSQLSDB_COORDINATE.getGroupId()));
    }

    private static boolean hasJUnit4Dependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(JUNIT4_COORDINATE.getArtifactId())
            .setGroupId(JUNIT4_COORDINATE.getGroupId()));
    }

    private boolean hasDBRiderDependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(DBRIDER_COORDINATE.getArtifactId())
            .setGroupId(DBRIDER_COORDINATE.getGroupId()));
    }

    private boolean hasTestResources() {
        Project project = getFaceted();
        DirectoryResource testResources = project.getFacet(ResourcesFacet.class).getTestResourceDirectory();
        boolean hasDatasetsDir = testResources.getChild("datasets").exists();
        if (!hasDatasetsDir) {
            return false;
        }
        DirectoryResource metaInf = testResources.getOrCreateChildDirectory("META-INF");
        boolean hasTestDeltaSpikeProperties = metaInf.getChild("apache-deltaspike.properties").exists();
        if (!hasTestDeltaSpikeProperties) {
            return false;
        }
        boolean hasTestBeansXml = metaInf.getChild("beans.xml").exists();
        return hasTestBeansXml;
    }

    private void addTestResources() {
        Project project = getFaceted();
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
                    new File(testMetaInf.getFullyQualifiedName() + "/beans.xml")));
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

    }

}
