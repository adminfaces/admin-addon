package com.github.adminfaces.addon.ui;

/**
 * @author rmpestano
 */
import com.github.adminfaces.addon.facet.AdminFacesTestHarnessFacet;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.parser.java.resources.JavaResource;
import org.jboss.forge.addon.parser.java.resources.JavaResourceVisitor;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.visit.VisitContext;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UISelectMany;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.jboss.forge.addon.resource.FileResource;

@FacetConstraint(AdminFacesTestHarnessFacet.class)
public class AdminFacesNewServiceTestCommand extends AbstractProjectCommand {

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    @WithAttributes(label = "Target services", description = "Select services to create the integration tests.", required = true, type = InputType.CHECKBOX)
    private UISelectMany<JavaClassSource> targetServices;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass()).name("AdminFaces: New test from service").category(Categories.create("AdminFaces"))
            .description("Creates integration tests for AdminFaces services.");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        UIContext context = builder.getUIContext();
        Project project = getSelectedProject(context);
        final List<JavaClassSource> services = getAllServices(project);
        targetServices.setValueChoices(services);
        targetServices.setItemLabelConverter((JavaClassSource source)
            -> source.getQualifiedName());
        builder.add(targetServices);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        final Project project = getSelectedProject(context) != null ? getSelectedProject(context)
            : getSelectedProject(context.getUIContext());

        List<Result> results = new ArrayList<>();

        results.add(Results.success("New services tests created successfully"));
        return Results.aggregate(results);
    }

    public List<JavaClassSource> getAllServices(Project project) {
        final List<JavaClassSource> result = new ArrayList<>();
        JavaSourceFacet javaSourceFacet = project.getFacet(JavaSourceFacet.class);
        List<JavaSourceFacet> currentServiceTests = new ArrayList<>();

        javaSourceFacet.visitJavaTestSources(new JavaResourceVisitor() {
            @Override
            public void visit(VisitContext context, JavaResource javaResource) {
                if (javaResource.getName().endsWith("It.java")) {
                    currentServiceTests.add((JavaSourceFacet) javaResource);
                }
            }
        });
        javaSourceFacet.visitJavaSources(new JavaResourceVisitor() {
            @Override
            public void visit(VisitContext context, JavaResource resource) {
                try {
                    JavaType<?> type = resource.getJavaType();
                    if (type.isClass() && type.hasAnnotation(Stateless.class) && !currentServiceTests.contains((JavaClassSource)type)) {
                        JavaClassSource classSource = (JavaClassSource) type;
                        if (classSource.hasImport("com.github.adminfaces.persistence.service.CrudService")) {
                            result.add((JavaClassSource) type);
                        }
                    }
                } catch (FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        return result;
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
