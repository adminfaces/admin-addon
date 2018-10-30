package com.github.admin.addon.facet;

import com.github.admin.addon.config.AdminConfiguration;
import org.jboss.forge.addon.projects.ProjectFacet;

/**
 * The AdminFaces Facet
 *
 * @author <a href="mailto:rmpestano@gmail.com">Rafael Pestano</a>
 */
public interface AdminFacet extends ProjectFacet {

  AdminConfiguration getConfiguration();

  void setConfiguration(AdminConfiguration configuration);

}
