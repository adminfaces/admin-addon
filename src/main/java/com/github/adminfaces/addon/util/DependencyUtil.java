package com.github.adminfaces.addon.util;

import static com.github.adminfaces.addon.util.AdminScaffoldUtils.getService;

import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

import com.github.adminfaces.addon.util.Constants.Versions;

/**
 * Created by rmpestano on 25/02/17.
 */
public class DependencyUtil {

    private static final String ADMIN_FACES_GROUP_ID = "com.github.adminfaces";

    public static final Coordinate ADMIN_THEME_COORDINATE = CoordinateBuilder.create().setGroupId(ADMIN_FACES_GROUP_ID).setArtifactId("admin-theme");
    public static final Coordinate ADMIN_TEMPLATE_COORDINATE = CoordinateBuilder.create().setGroupId(ADMIN_FACES_GROUP_ID).setArtifactId("admin-template");
    public static final Coordinate ADMIN_PERSISTENCE_COORDINATE = CoordinateBuilder.create().setGroupId(ADMIN_FACES_GROUP_ID).setArtifactId("admin-persistence");
    public static final Coordinate HSQLSDB_COORDINATE = CoordinateBuilder.create().setGroupId("org.hsqldb").setArtifactId("hsqldb").setVersion("2.3.5");
    public static final Coordinate JUNIT4_COORDINATE = CoordinateBuilder.create().setGroupId("junit").setArtifactId("junit");
    public static final Coordinate ASSERTJ_COORDINATE = CoordinateBuilder.create().setGroupId("org.assertj").setArtifactId("assertj-core").setVersion("3.6.2");
    public static final Coordinate DBRIDER_COORDINATE = CoordinateBuilder.create().setGroupId("com.github.database-rider").setArtifactId("rider-cdi");
    public static final Coordinate PRIMEFACES_EXTENSIONS_COORDINATE = CoordinateBuilder.create().setGroupId("org.primefaces.extensions")
        .setArtifactId("primefaces-extensions").setVersion("6.2.8");
    public static final Coordinate DELTASPIKE_CORE_API_COORDINATE = CoordinateBuilder.create().setGroupId("org.apache.deltaspike.core")
        .setArtifactId("deltaspike-core-api").setVersion(Versions.DELTASPIKE);
    public static final Coordinate DELTASPIKE_TESTCONTROL_COORDINATE = CoordinateBuilder.create().setGroupId("org.apache.deltaspike.modules").setArtifactId("deltaspike-test-control-module-impl").setVersion(Versions.DELTASPIKE);
    public static final Coordinate DELTASPIKE_CDICONTROL_COORDINATE = CoordinateBuilder.create().setGroupId("org.apache.deltaspike.cdictrl").setArtifactId("deltaspike-cdictrl-owb").setVersion(Versions.DELTASPIKE);
    public static final Coordinate OPENWEBBEANS_COORDINATE = CoordinateBuilder.create().setGroupId("org.apache.openwebbeans").setArtifactId("openwebbeans-impl").setVersion(Versions.OPENWEBBEANS);
    public static final Coordinate HIBERNATE_CORE_COORDINATE = CoordinateBuilder.create().setGroupId("org.hibernate").setArtifactId("hibernate-core").setVersion(Versions.HIBERNATE);
    public static final Coordinate HIBERNATE_ENTITYMANAGER_COORDINATE = CoordinateBuilder.create().setGroupId("org.hibernate").setArtifactId("hibernate-entitymanager").setVersion(Versions.HIBERNATE);


    public Coordinate getLatestVersion(Coordinate coordinate) {
        return getService(DependencyResolver.class).resolveVersions(DependencyQueryBuilder.create(coordinate))
            .stream()
            .filter(d -> !d.getVersion().contains("SNAPSHOT"))
            .reduce((a, b) -> b)
            .orElse(null);
    }

    public void installDependency(DependencyFacet facet, DependencyBuilder dependency) {
        if (!facet.hasDirectDependency(dependency)) {
            facet.addDirectDependency(dependency);
        }
    }

    public void reInstallDependency(DependencyFacet facet, DependencyBuilder dependency) {
        if (!facet.hasDirectDependency(dependency)) {
            facet.addDirectDependency(dependency);
        } else {
            facet.removeDependency(dependency);
            facet.addDirectDependency(dependency);
        }
    }

}
