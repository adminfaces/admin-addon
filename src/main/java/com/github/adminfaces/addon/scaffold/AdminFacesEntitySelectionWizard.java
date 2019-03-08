package com.github.adminfaces.addon.scaffold;

import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.jboss.forge.addon.javaee.jpa.JPAFacet;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.scaffold.spi.ResourceCollection;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.util.Refactory;
import org.jboss.shrinkwrap.descriptor.api.persistence.PersistenceCommonDescriptor;

public class AdminFacesEntitySelectionWizard extends AbstractProjectCommand implements UIWizardStep {

    private UISelectMany<JavaClassSource> entities;
    private UIInput<Boolean> generateEqualsAndHashCode;

    @Inject
    private ProjectFactory projectFactory;

    @Override
    @SuppressWarnings("unchecked")
    public void initializeUI(final UIBuilder builder) throws Exception {
        InputComponentFactory factory = builder.getInputComponentFactory();
        entities = factory.createSelectMany("entities", JavaClassSource.class).setLabel("Entities").setRequired(true)
            .setDescription("The JPA entities to use as the basis for generating the scaffold.");
        generateEqualsAndHashCode = factory.createInput("generateEqualsAndHashCode", Boolean.class)
            .setLabel("Generate missing .equals() and .hashCode() methods").setDescription(
            "If enabled, entities missing an .equals() or .hashCode() method will be updated to provide them");

        Project project = getSelectedProject(builder);

        JPAFacet<PersistenceCommonDescriptor> persistenceFacet = project.getFacet(JPAFacet.class);
        entities.setValueChoices(persistenceFacet.getAllEntities());
        entities.setItemLabelConverter(source -> source.getQualifiedName());
        builder.add(entities);
        generateEqualsAndHashCode.setEnabled(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                for (JavaClassSource javaSource : entities.getValue()) {
                    if (!javaSource.hasMethodSignature("hashCode")
                        || !javaSource.hasMethodSignature("equals", Object.class)) {
                        return true;
                    }
                }
                return false;
            }
        });
        builder.add(generateEqualsAndHashCode);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        UIContext uiContext = context.getUIContext();
        Map<Object, Object> attributeMap = uiContext.getAttributeMap();
        ResourceCollection resourceCollection = new ResourceCollection();
        if (entities.getValue() != null) {
            for (JavaClassSource klass : entities.getValue()) {
                Project project = getSelectedProject(uiContext);
                JavaSourceFacet javaSource = project.getFacet(JavaSourceFacet.class);
                Resource<?> resource = javaSource.getJavaResource(klass);
                if (resource != null) {
                    resourceCollection.addToCollection(resource);
                }
            }
        }

        attributeMap.put(ResourceCollection.class, resourceCollection);
        return null;
    }

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass()).name("Select JPA entities")
            .description("Select the JPA entities to be used for scaffolding.");
    }

    @Override
    public boolean isEnabled(UIContext context) {
        return true;
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        for (JavaClassSource javaSource : entities.getValue()) {
            UIContext uiContext = context.getUIContext();
            Project project = getSelectedProject(uiContext);
            JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
            if (!javaSource.hasMethodSignature("hashCode")) {
                if (generateEqualsAndHashCode.getValue()) {
                    if (javaSource.getField("id") != null) {
                        Refactory.createHashCode(javaSource, javaSource.getField("id"));
                    } else {
                        Refactory.createHashCode(javaSource,
                            javaSource.getFields().toArray(new FieldSource[javaSource.getFields().size()]));
                    }

                }
            }

            if (!javaSource.hasMethodSignature("equals", Object.class)) {
                if (generateEqualsAndHashCode.getValue()) {
                    if (javaSource.getField("id") != null) {
                        Refactory.createEquals(javaSource, javaSource.getField("id"));
                    } else {
                        Refactory.createEquals(javaSource,
                            javaSource.getFields().toArray(new FieldSource[javaSource.getFields().size()]));
                    }
                }
            }
            javaSourceFacet.saveJavaSource(javaSource);
        }

        return null;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

}
