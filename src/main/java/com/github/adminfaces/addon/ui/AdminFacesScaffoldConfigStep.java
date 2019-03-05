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

import static com.github.adminfaces.addon.scaffold.config.ScaffoldConfigLoader.YML_DUMP_OPTIONS;
import java.util.Arrays;
import java.util.Map;
import javax.inject.Inject;
import org.jboss.forge.addon.facets.FacetFactory;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.InputComponentFactory;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;

/**
 *
 * @author rmpestano
 */
import com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum;
import com.github.adminfaces.addon.scaffold.model.EntityConfig;
import com.github.adminfaces.addon.scaffold.model.GlobalConfig;
import java.io.InputStream;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.result.Results;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class AdminFacesScaffoldConfigStep extends AbstractProjectCommand implements UIWizardStep {

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    private GlobalConfig globalConfig;

    private EntityConfig entityConfig;

    private FileResource<?> scaffoldConfigFile;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        UIContext context = builder.getUIContext();
        Map<Object, Object> attributeMap = context.getAttributeMap();
        InputComponentFactory componentFactory = builder.getInputComponentFactory();
        scaffoldConfigFile = (FileResource<?>) attributeMap.get(FileResource.class);
        if (scaffoldConfigFile.getName().endsWith("global-config.yml")) {
            try (InputStream entityConfigStream = scaffoldConfigFile.getResourceInputStream()) {
                globalConfig = new Yaml().loadAs(entityConfigStream, GlobalConfig.class);
            }
            //global config
            UISelectOne<ComponentTypeEnum> toOneComponentType = componentFactory.createSelectOne("ToOneComponentType", ComponentTypeEnum.class)
                .setDescription("Component type to be used in toOne entity associations.")
                .setRequired(true)
                .setDefaultValue(globalConfig.getToOneComponentType())
                .setValueChoices(Arrays.asList(ComponentTypeEnum.AUTOCOMPLETE, ComponentTypeEnum.SELECT_ONE_MENU));
            builder.add(toOneComponentType);

            toOneComponentType.addValueChangeListener((ValueChangeEvent event) -> {
                globalConfig.setToOneComponentType((ComponentTypeEnum) event.getNewValue());
            });

            UISelectOne<ComponentTypeEnum> toManyComponentType = componentFactory.createSelectOne("ToManyComponentType", ComponentTypeEnum.class)
                .setDescription("Component type to be used in toMany entity associations.")
                .setRequired(true)
                .setDefaultValue(globalConfig.getToOneComponentType())
                .setValueChoices(Arrays.asList(ComponentTypeEnum.CHECKBOXMENU, ComponentTypeEnum.SELECT_MANY_MENU));
            builder.add(toManyComponentType);

            toManyComponentType.addValueChangeListener((ValueChangeEvent event) -> {
                globalConfig.setToManyComponentType((ComponentTypeEnum) event.getNewValue());
            });

            UISelectOne<ComponentTypeEnum> dateComponentType = componentFactory.createSelectOne("DateComponentType", ComponentTypeEnum.class)
                .setDescription("Component type to be used in date fields.")
                .setRequired(true)
                .setDefaultValue(globalConfig.getDateComponentType())
                .setValueChoices(Arrays.asList(ComponentTypeEnum.CALENDAR, ComponentTypeEnum.DATEPICKER));
            builder.add(dateComponentType);

            dateComponentType.addValueChangeListener((ValueChangeEvent event) -> {
                globalConfig.setDateComponentType((ComponentTypeEnum) event.getNewValue());
            });

            UIInput<Boolean> datatableEditable = componentFactory.createInput("Datatable editable", Boolean.class)
                .setDefaultValue(globalConfig.getDatatableEditable())
                .setRequired(true)
                .setDescription("When true, generates editable datatable in list page");
            builder.add(datatableEditable);

            datatableEditable.addValueChangeListener((ValueChangeEvent event) -> {
                globalConfig.setDatatableEditable((Boolean) event.getNewValue());
            });

            UIInput<Boolean> datatableReflow = componentFactory.createInput("Datatable reflow", Boolean.class)
                .setDefaultValue(globalConfig.getDatatableReflow())
                .setRequired(true)
                .setDescription("When true, will set datatable reflow attribute on list page");
            builder.add(datatableReflow);

            datatableReflow.addValueChangeListener((ValueChangeEvent event) -> {
                globalConfig.setDatatableReflow((Boolean) event.getNewValue());
            });

            UIInput<Integer> inputSize = componentFactory.createInput("Input size", Integer.class)
                .setDefaultValue(globalConfig.getInputSize())
                .setRequired(true)
                .setDescription("The input size is used to decide whether the scaffold will use a inputtext or textarea component. When entity field length attribute is greater than input size then a textarea will be rendered.");
            builder.add(inputSize);

            inputSize.addValueChangeListener((ValueChangeEvent event) -> {
                globalConfig.setInputSize((Integer) event.getNewValue());
            });

            UIInput<String> menuIcon = componentFactory.createInput("Menu icon", String.class)
                .setDefaultValue(globalConfig.getMenuIcon())
                .setRequired(true)
                .setDescription("Font awesome icon to be used in menu entries.");
            builder.add(menuIcon);

            menuIcon.addValueChangeListener((ValueChangeEvent event) -> {
                globalConfig.setMenuIcon(event.getNewValue().toString());
            });
        } else {
            
        }
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        String configFileContent = null;
        if (globalConfig != null) {
            configFileContent = new Yaml(YML_DUMP_OPTIONS).dump(globalConfig);
        } else {
            configFileContent = new Yaml(YML_DUMP_OPTIONS).dump(entityConfig);
        }
        scaffoldConfigFile.setContents(configFileContent);
        return Results.success(scaffoldConfigFile.getName() + " updated successfully!");
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        // This is the last step
        return null;
    }

    @Override
    protected boolean isProjectRequired() {
        return false;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

}
