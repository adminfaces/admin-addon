package com.github.adminfaces.addon.facet;

import static com.github.adminfaces.addon.util.DependencyUtil.*;

import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.forge.addon.dependencies.builder.DependencyBuilder;
import org.jboss.forge.addon.facets.AbstractFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;

import com.github.adminfaces.addon.util.DependencyUtil;

/**
 * The implementation of the {@link AdminFacet}
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
public class AdminFacetImpl extends AbstractFacet<Project> implements AdminFacet {
     
    private Logger logger = Logger.getLogger(AdminFacetImpl.class.getName());

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
        
        DependencyBuilder primefacesExtensionsDependency = DependencyBuilder.create()
            .setCoordinate(PRIMEFACES_EXTENSIONS_COORDINATE);
        
        dependencyUtil.installDependency(dependencyFacet, adminThemeDependency);

        dependencyUtil.installDependency(dependencyFacet, adminTemplateDependency);
        
        dependencyUtil.installDependency(dependencyFacet, primefacesExtensionsDependency);//only for gravatar

    }


    @Override
    public boolean isInstalled() {
        DependencyFacet facet = getFaceted().getFacet(DependencyFacet.class);
        return facet.hasDirectDependency(DependencyBuilder.create()
                .setArtifactId(ADMIN_TEMPLATE_COORDINATE.getArtifactId())
                .setGroupId(ADMIN_TEMPLATE_COORDINATE.getGroupId()));
    }

}
