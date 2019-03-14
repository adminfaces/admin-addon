package com.github.adminfaces.addon.facet;

import static com.github.adminfaces.addon.util.DependencyUtil.ADMIN_PERSISTENCE_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.DBRIDER_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.DELTASPIKE_CDICONTROL_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.DELTASPIKE_TESTCONTROL_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.HIBERNATE_CORE_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.HIBERNATE_ENTITYMANAGER_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.HSQLSDB_COORDINATE;
import static com.github.adminfaces.addon.util.DependencyUtil.JUNIT4_COORDINATE;

import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;

/**
 * The implementation of the {@link AdminFacesFacet}
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
public class AdminFacesTestHarnessFacetImpl extends AbstractFacet<Project> implements AdminFacesTestHarnessFacet {

    @Override
    public boolean install() {
        return isInstalled();
    }
    
    @Override
    public boolean isInstalled() {
        DependencyFacet dependencyFacet = getFaceted().getFacet(DependencyFacet.class);
        return hasAdminPersistenceDependency(dependencyFacet) && hasHibernateDependencies(dependencyFacet)
            && hasHsqldbDependency(dependencyFacet) && hasJUnit4Dependency(dependencyFacet)
            && hasDBRiderDependency(dependencyFacet) && hasDeltaSpikeTestControlDependencies(dependencyFacet)
            && hasTestResources() && hasTestEntityManagerProducer();
    }

    private boolean hasAdminPersistenceDependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(ADMIN_PERSISTENCE_COORDINATE.getArtifactId())
            .setGroupId(ADMIN_PERSISTENCE_COORDINATE.getGroupId()));
    }

    private boolean hasHsqldbDependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(HSQLSDB_COORDINATE.getArtifactId())
            .setGroupId(HSQLSDB_COORDINATE.getGroupId()));
    }

    private boolean hasJUnit4Dependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(JUNIT4_COORDINATE.getArtifactId())
            .setGroupId(JUNIT4_COORDINATE.getGroupId()));
    }

    private boolean hasDeltaSpikeTestControlDependencies(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(DELTASPIKE_TESTCONTROL_COORDINATE.getArtifactId())
            .setGroupId(DELTASPIKE_TESTCONTROL_COORDINATE.getGroupId()))
            && facet.hasDirectDependency(DependencyBuilder.create()
                .setArtifactId(DELTASPIKE_CDICONTROL_COORDINATE.getArtifactId())
                .setGroupId(DELTASPIKE_CDICONTROL_COORDINATE.getGroupId()));
    }

    private boolean hasDBRiderDependency(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(DBRIDER_COORDINATE.getArtifactId())
            .setGroupId(DBRIDER_COORDINATE.getGroupId()));
    }

    private boolean hasHibernateDependencies(DependencyFacet facet) {
        return facet.hasDirectDependency(DependencyBuilder.create()
            .setArtifactId(HIBERNATE_CORE_COORDINATE.getArtifactId())
            .setGroupId(HIBERNATE_CORE_COORDINATE.getGroupId()))
            && facet.hasDirectDependency(DependencyBuilder.create()
                .setArtifactId(HIBERNATE_ENTITYMANAGER_COORDINATE.getArtifactId())
                .setGroupId(HIBERNATE_ENTITYMANAGER_COORDINATE.getGroupId()));
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

    private boolean hasTestEntityManagerProducer() {
        Project project = getFaceted();
        JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
        JavaResource testEntityManagerProducer = javaSourceFacet.getTestJavaResource(javaSourceFacet.getBasePackage() + ".infra.TestEntityManagerProducer.java");
        return testEntityManagerProducer.exists();
    }

}
