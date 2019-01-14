package com.github.adminfaces.addon.scaffold.metamodel;

import org.jboss.forge.addon.dependencies.Coordinate;
import org.jboss.forge.addon.dependencies.DependencyRepository;
import org.jboss.forge.addon.dependencies.builder.CoordinateBuilder;
import org.jboss.forge.addon.javaee.jpa.MetaModelProvider;

public class AdminFacesMetaModelProvider implements MetaModelProvider {

	@Override
	public Coordinate getAptCoordinate() {
		return CoordinateBuilder.create().setGroupId("org.hibernate").setArtifactId("hibernate-jpamodelgen");
	}

	@Override
	public String getProcessor() {
		return "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor";
	}

	@Override
	public String getCompilerArguments() {
		return null;
	}

	@Override
	public DependencyRepository getAptPluginRepository() {
		return null;
	}
}
