package com.github.admin.addon.facet;

import static com.github.admin.addon.util.DependencyUtil.ADMIN_TEMPLATE_COORDINATE;
import static com.github.admin.addon.util.DependencyUtil.ADMIN_THEME_COORDINATE;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

import com.github.admin.addon.config.AdminConfiguration;
import com.github.admin.addon.util.DependencyUtil;

/**
 * The implementation of the {@link AdminFacet}
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
public class AdminFacetImpl extends AbstractFacet<Project> implements AdminFacet {

    @Inject
    private Logger logger;

    @Inject
    private AdminConfiguration configuration;

    @Inject
    private DependencyUtil dependencyUtil;

    @Override
    public boolean install() {
        addAdminFacesDependencies();
        return isInstalled();
    }

    private void addAdminFacesDependencies() {
        DependencyFacet dependencyFacet = getFaceted().getFacet(DependencyFacet.class);

        DependencyBuilder adminThemeDependency = DependencyBuilder.create()
                .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_THEME_COORDINATE));

        DependencyBuilder adminTemplateDependency = DependencyBuilder.create()
                .setCoordinate(dependencyUtil.getLatestVersion(ADMIN_TEMPLATE_COORDINATE));
        
        dependencyUtil.installDependency(dependencyFacet, adminThemeDependency);

        dependencyUtil.installDependency(dependencyFacet, adminTemplateDependency);

    }


    @Override
    public boolean isInstalled() {
        DependencyFacet facet = getFaceted().getFacet(DependencyFacet.class);
        return facet.hasDirectDependency(DependencyBuilder.create()
                .setArtifactId(ADMIN_TEMPLATE_COORDINATE.getArtifactId())
                .setGroupId(ADMIN_TEMPLATE_COORDINATE.getGroupId()));
    }

    @Override
    public AdminConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(AdminConfiguration configuration) {
        this.configuration = configuration;
    }


}
