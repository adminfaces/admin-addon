package com.github.adminfaces.addon.util;

import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.DependencyResolver;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.dependencies.builder.DependencyQueryBuilder;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

import javax.inject.Inject;

/**
 * Created by rmpestano on 25/02/17.
 */
public class DependencyUtil {

    private static final String ADMIN_FACES_GROUP_ID = "com.github.adminfaces";

    public static final Coordinate ADMIN_THEME_COORDINATE = CoordinateBuilder.create().setGroupId(ADMIN_FACES_GROUP_ID).setArtifactId("admin-theme");
    public static final Coordinate ADMIN_TEMPLATE_COORDINATE = CoordinateBuilder.create().setGroupId(ADMIN_FACES_GROUP_ID).setArtifactId("admin-template");
    public static final Coordinate ADMIN_PERSISTENCE_COORDINATE = CoordinateBuilder.create().setGroupId(ADMIN_FACES_GROUP_ID).setArtifactId("admin-persistence");

    public static final Coordinate PRIMEFACES_EXTENSIONS_COORDINATE = CoordinateBuilder.create().setGroupId("org.primefaces.extensions")
        .setArtifactId("primefaces-extensions").setVersion("6.2.8");

    @Inject
    private DependencyResolver resolver;

    public Coordinate getLatestVersion(Coordinate coordinate) {
        return resolver.resolveVersions(DependencyQueryBuilder.create(coordinate))
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

}
