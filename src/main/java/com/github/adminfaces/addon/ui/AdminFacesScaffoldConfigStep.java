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
import com.github.adminfaces.addon.scaffold.model.FieldConfig;
import com.github.adminfaces.addon.scaffold.model.GlobalConfig;
import com.github.adminfaces.addon.util.AdminScaffoldUtils;
import com.github.adminfaces.addon.util.Constants;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.MetadataFacet;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.events.ValueChangeEvent;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.yaml.snakeyaml.Yaml;

public class AdminFacesScaffoldConfigStep extends AbstractProjectCommand implements UIWizardStep {

    @Inject
    private FacetFactory facetFactory;

    @Inject
    private ProjectFactory projectFactory;

    private GlobalConfig globalConfig;

    private EntityConfig entityConfig;

    private FileResource<?> scaffoldConfigFile;

    private FieldConfig fieldConfig;

    private UIInput<Boolean> hidden;

    private UIInput<Integer> length;
    
    private UIInput<Boolean> required;
    
    private UISelectOne<ComponentTypeEnum> type;

    @Override
    public Metadata getMetadata(UIContext context) {
        return Metadata.from(super.getMetadata(context), getClass()).name("AdminFaces: Scaffold config")
            .description(context.getAttributeMap().get(FileResource.class) != null ? "Configuration file: "
                + ((FileResource) context.getAttributeMap().get(FileResource.class)).getName() : "");
    }

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        UIContext context = builder.getUIContext();
        Map<Object, Object> attributeMap = context.getAttributeMap();
        InputComponentFactory componentFactory = builder.getInputComponentFactory();
        scaffoldConfigFile = (FileResource<?>) attributeMap.get(FileResource.class);
        fieldConfig = null;
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
                .setDefaultValue(globalConfig.getToManyComponentType())
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
        } else { //entity config
            try (InputStream entityConfigStream = scaffoldConfigFile.getResourceInputStream()) {
                entityConfig = new Yaml().loadAs(entityConfigStream, EntityConfig.class);
            }
            Project project = getSelectedProject(builder.getUIContext());
            String sourceFolder = AdminScaffoldUtils.resolveSourceFolder(project);
            MetadataFacet metadataFacet = project.getFacet(MetadataFacet.class);
            String entityPackage = metadataFacet.getProjectGroupName() + "." + Constants.Packages.MODEL + "." + scaffoldConfigFile.getName().substring(0, scaffoldConfigFile.getName().indexOf("."));
            JavaClassSource entitySource = Roaster.parse(JavaClassSource.class, new File(sourceFolder + "/" + entityPackage.replace(".", "/") + ".java"));
            UISelectOne<String> displayField = componentFactory.createSelectOne("Display field", String.class)
                .setDefaultValue(entityConfig.getDisplayField())
                .setRequired(true)
                .setDescription("Field used to display this entity on pages, e.g select one menu item label.");

            List<String> availableFields = entitySource.getFields()
                .stream()
                .filter(AdminScaffoldUtils::isValidDisplayField)
                .map(f -> f.getName())
                .collect(Collectors.toList());
            displayField.setValueChoices(availableFields);
            builder.add(displayField);

            displayField.addValueChangeListener((ValueChangeEvent event) -> {
                entityConfig.setDisplayField(event.getNewValue().toString());
            });

            UIInput<Boolean> datatableEditable = componentFactory.createInput("Datatable editable", Boolean.class)
                .setDefaultValue(entityConfig.getDatatableEditable())
                .setRequired(true)
                .setDescription("When true, generates editable datatable in list page");
            builder.add(datatableEditable);

            datatableEditable.addValueChangeListener((ValueChangeEvent event) -> {
                entityConfig.setDatatableEditable((Boolean) event.getNewValue());
            });

            UIInput<Boolean> datatableReflow = componentFactory.createInput("Datatable reflow", Boolean.class)
                .setDefaultValue(entityConfig.getDatatableReflow())
                .setRequired(true)
                .setDescription("When true, will set datatable reflow attribute on list page");
            builder.add(datatableReflow);

            datatableReflow.addValueChangeListener((ValueChangeEvent event) -> {
                entityConfig.setDatatableReflow((Boolean) event.getNewValue());
            });

            UIInput<String> menuIcon = componentFactory.createInput("Menu icon", String.class)
                .setDefaultValue(entityConfig.getMenuIcon())
                .setRequired(true)
                .setDescription("Font awesome icon to be used in menu entries.");
            builder.add(menuIcon);

            datatableEditable.addValueChangeListener((ValueChangeEvent event) -> {
                entityConfig.setDatatableEditable((Boolean) event.getNewValue());
            });

            //field config
            hidden = componentFactory.createInput("Hidden", Boolean.class)
                .setEnabled(false);

            length = componentFactory.createInput("Length", Integer.class)
                .setEnabled(false);
            
            required = componentFactory.createInput("Required", Boolean.class)
                .setEnabled(false);
            
            type = componentFactory.createSelectOne("Type", ComponentTypeEnum.class)
                .setValueChoices(Arrays.stream(ComponentTypeEnum.values()).collect(Collectors.toList()))
                .setEnabled(false);
            
            List<FieldConfig> entityFieldConfigs = new ArrayList<>();
            entityFieldConfigs.add(null);
            entityFieldConfigs.addAll(entityConfig.getFields());
            UISelectOne<FieldConfig> fieldConfigList = componentFactory.createSelectOne("Choice field to change", FieldConfig.class)
                .setDescription("Select an entity field to edit its scaffold configuration.")
                .setRequired(false)
                .setValueChoices(entityFieldConfigs);

            fieldConfigList.setItemLabelConverter((FieldConfig source) -> source == null ? "Select an option" : source.getName());

            fieldConfigList.addValueChangeListener((ValueChangeEvent event) -> {
                fieldConfig = (FieldConfig) event.getNewValue();
                if (fieldConfig != null) {
                    hidden.setEnabled(true)
                        .setDescription(String.format("When true the field '%s' will be ignored on AdminFaces scaffold.", fieldConfig.getName()))
                        .setValue(fieldConfig.getHidden());

                    hidden.addValueChangeListener((ValueChangeEvent evt) -> {
                        fieldConfig.setHidden((Boolean) evt.getNewValue());
                    });
                    required.setEnabled(true)
                        .setDescription(String.format("When true the field '%s' will be required on AdminFaces scaffold generated pages.", fieldConfig.getName()))
                        .setValue(fieldConfig.getRequired());

                    required.addValueChangeListener((ValueChangeEvent evt) -> {
                        fieldConfig.setRequired((Boolean) evt.getNewValue());
                    });
                    length.setEnabled(true)
                        .setDescription(String.format("Set field '%s' length to be used on AdminFaces scaffold generated pages.", fieldConfig.getName()))
                        .setValue(fieldConfig.getLength());

                    length.addValueChangeListener((ValueChangeEvent evt) -> {
                        fieldConfig.setLength((Integer) evt.getNewValue());
                    });
                    type.setEnabled(true)
                        .setDescription(String.format("Set field '%s' component type to be used on AdminFaces scaffold generated pages.", fieldConfig.getName()))
                        .setValue(fieldConfig.getType());

                    type.addValueChangeListener((ValueChangeEvent evt) -> {
                        fieldConfig.setType((ComponentTypeEnum) evt.getNewValue());
                    });
                } else {
                    hidden.setEnabled(false)
                        .setNote("");
                    length.setEnabled(false)
                        .setNote("");
                    required.setEnabled(false)
                        .setNote("");
                    type.setEnabled(false)
                        .setNote("");
                }
            });
            builder.add(fieldConfigList)
            .add(hidden).add(length).add(required).add(type);
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
