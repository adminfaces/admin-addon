package com.github.adminfaces.addon.scaffold;

import org.jboss.forge.addon.resource.Resource;

/**
 * A strategy defining the manner in which template resources interact with generated resources.
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
public interface TemplateStrategy
{
   /**
    * Return true if this {@link TemplateStrategy} is compatible with the given template {@link Resource}.
    */
   boolean compatibleWith(Resource<?> template);

   /**
    * Return the path by which the given {@link Resource} template should be referenced when constructing generated
    * resources.
    */
   String getReferencePath(Resource<?> template);

   /**
    * Return the default template to be used when generating resource, or null if none should be used.
    */
   Resource<?> getDefaultTemplate();
}
