/*
 * The MIT License
 *
 * Copyright 2019 rmpestano.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.adminfaces.addon.ui;

import org.jboss.forge.addon.facets.constraints.FacetConstraint;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.ui.command.PrerequisiteCommandsProvider;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.wizard.UIWizard;
import org.jboss.forge.addon.ui.result.navigation.NavigationResultBuilder;
import com.github.adminfaces.addon.facet.AdminFacesFacet;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.resource.Resource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.context.UISelection;
import org.jboss.forge.addon.ui.hints.InputType;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.UICommandMetadata;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;

/**
 *
 * @author rmpestano
 */
@FacetConstraint(JavaSourceFacet.class)
public class AdminScaffoldConfigWizard extends AbstractProjectCommand implements UIWizard, PrerequisiteCommandsProvider {

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    @Inject
    @WithAttributes(label = "Select configuration file", description = "Target scaffold configuration file to edit", required = true, type = InputType.DROPDOWN)
    private UISelectOne<FileResource<?>> configFile;

    @Override
    public UICommandMetadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass()).name("AdminFaces: Scaffold config").category(Categories.create("AdminFaces"))
            .description("Configure AdminFaces scaffold.");
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        return Results.success();
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        loadConfigFiles(builder.getUIContext());
        builder.add(configFile);
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        context.getUIContext().getAttributeMap().put(FileResource.class, configFile.getValue());
        return Results.navigateTo(AdminScaffoldConfigStep.class);
    }

    @Override
    public NavigationResult getPrerequisiteCommands(UIContext context) {
        NavigationResultBuilder builder = NavigationResultBuilder.create();
        Project project = getSelectedProject(context);
        if (project != null && !project.hasFacet(AdminFacesFacet.class)) {
            builder.add(AdminSetupCommand.class);
        }
        return builder.build();
    }

    private void loadConfigFiles(UIContext context) {
        UISelection<FileResource<?>> selection = context.getInitialSelection();
        final Project project = getSelectedProject(context);
        DirectoryResource resources = project.getFacet(ResourcesFacet.class).getResourceDirectory();
        DirectoryResource scaffoldDir = resources.getChildDirectory("scaffold");
        final List<FileResource<?>> scaffoldConfigList = scaffoldDir.listResources((Resource<?> file) -> file.getName().endsWith("yml"))
            .stream()
            .map(r -> (FileResource<?>) r)
            .collect(Collectors.toList());
        configFile.setValueChoices(scaffoldConfigList);
        configFile.setItemLabelConverter((FileResource<?> source)
            -> source.getFullyQualifiedName().substring(source.getFullyQualifiedName().indexOf(project.getRoot().getName())));
        int selectionIndex = -1;
        if (!selection.isEmpty()) {
            selectionIndex = scaffoldConfigList.indexOf(selection.get());
        }
        if (selectionIndex != -1) {
            configFile.setDefaultValue(scaffoldConfigList.get(selectionIndex));
        }
    }

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

}
