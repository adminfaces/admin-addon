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
package com.github.adminfaces.addon.scaffold.model;

import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.*;
import static org.mockito.Mockito.*;
import com.github.adminfaces.addon.scaffold.config.ScaffoldConfigLoader;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.forge.addon.dependencies.Dependency;

import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.DependencyFacet;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author rmpestano
 */
@RunWith(JUnit4.class)
public class ScaffoldConfigLoaderTest {

    Project project;

    @Before
    public void initGlobalConfig() throws IllegalArgumentException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, IllegalAccessException {
        //clean cached instance of global config
        Field globalConfigField = ScaffoldConfigLoader.class.getDeclaredField("globalConfig");
        globalConfigField.setAccessible(true);
        globalConfigField.set(null, null);
        project = mock(Project.class);
        DependencyFacet dependencyFacet = mock(DependencyFacet.class);
        doReturn(new ArrayList<Dependency>()).when(dependencyFacet).getEffectiveDependencies();
        doReturn(dependencyFacet).when(project).getFacet(DependencyFacet.class);
    }

    @Test
    public void shouldLoadGlobalConfig() {
        ResourcesFacet resourcesFacet = mock(ResourcesFacet.class);
        DirectoryResource directoryResource = mock(DirectoryResource.class);
        doReturn(directoryResource).when(resourcesFacet).getResourceDirectory();
        DirectoryResource scaffoldDir = mock(DirectoryResource.class);
        doReturn(scaffoldDir).when(directoryResource).getChildDirectory("scaffold");
        FileResource<?> globalConfigFile = mock(FileResource.class);
        doReturn(globalConfigFile).when(scaffoldDir).getChild(anyString());
        InputStream is = ScaffoldConfigLoader.class.getResourceAsStream("/scaffold/global-config.yml");
        doReturn(is).when(globalConfigFile).getResourceInputStream();
        doReturn(resourcesFacet).when(project).getFacet(ResourcesFacet.class);
        GlobalConfig globalConfig = ScaffoldConfigLoader.loadGlobalConfig(project);
        assertThat(globalConfig).isNotNull();
        assertThat(globalConfig).extracting("toOneComponentType", "toManyComponentType", "datatableEditable", "inputSize", "menuIcon")
            .contains(ComponentTypeEnum.AUTOCOMPLETE, ComponentTypeEnum.CHECKBOXMENU, false, 100, "fa fa-circle-o");
    }

    @Test
    public void shouldCreateEntityConfig() {
        FileResource<?> entityConfigFile = mock(FileResource.class);
        JavaSourceFacet sourceFacet = mock(JavaSourceFacet.class);
        String sourceDirPath = Paths.get("target/test-classes").toAbsolutePath().toString();
        DirectoryResource sourceDir = mock(DirectoryResource.class);
        doReturn(sourceDirPath).when(sourceDir).getFullyQualifiedName();
        doReturn(sourceDir).when(sourceFacet).getSourceDirectory();
        doReturn(false).when(entityConfigFile).exists();
        doReturn(null).when(entityConfigFile).setContents(anyString());
        DirectoryResource scaffoldDir = mock(DirectoryResource.class);
        doReturn(entityConfigFile).when(scaffoldDir).getChild(anyString());
        FileResource<?> globalConfigFile = mock(FileResource.class);
        doReturn(globalConfigFile).when(scaffoldDir).getChild("global-config.yml");
        InputStream is = ScaffoldConfigLoader.class.getResourceAsStream("/scaffold/global-config.yml");
        doReturn(is).when(globalConfigFile).getResourceInputStream();
        doReturn(sourceFacet).when(project).getFacet(JavaSourceFacet.class);
        ResourcesFacet resourcesFacet = mock(ResourcesFacet.class);
        DirectoryResource directoryResource = mock(DirectoryResource.class);
        doReturn(directoryResource).when(resourcesFacet).getResourceDirectory();
        doReturn(scaffoldDir).when(directoryResource).getChildDirectory("scaffold");
        doReturn(resourcesFacet).when(project).getFacet(ResourcesFacet.class);
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Speaker.java"));
        EntityConfig entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
        assertSpeakerEntityConfig(entityConfig);
        entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Talk.java"));
        entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
        assertTalkEntityConfig(entityConfig);
    }

    @Test
    public void shouldLoadEntityConfig() {
        DirectoryResource scaffoldDir = mock(DirectoryResource.class);
        FileResource<?> speakerFileResourceConfigFile = mock(FileResource.class);
        doReturn(true).when(speakerFileResourceConfigFile).exists();
        doReturn(null).when(speakerFileResourceConfigFile).setContents(anyString());
        InputStream speakerStream = getClass().getResourceAsStream("/scaffold/speaker.yml");
        doReturn(speakerStream).when(speakerFileResourceConfigFile).getResourceInputStream();
        doReturn(speakerFileResourceConfigFile).when(scaffoldDir).getChild("Speaker.yml");

        FileResource<?> talkFileResourceConfigFile = mock(FileResource.class);
        doReturn(true).when(talkFileResourceConfigFile).exists();
        doReturn(null).when(talkFileResourceConfigFile).setContents(anyString());
        InputStream talkStream = getClass().getResourceAsStream("/scaffold/talk.yml");
        doReturn(talkStream).when(talkFileResourceConfigFile).getResourceInputStream();
        doReturn(talkFileResourceConfigFile).when(scaffoldDir).getChild("Talk.yml");
        ResourcesFacet resourcesFacet = mock(ResourcesFacet.class);
        DirectoryResource directoryResource = mock(DirectoryResource.class);
        doReturn(directoryResource).when(resourcesFacet).getResourceDirectory();
        doReturn(scaffoldDir).when(directoryResource).getChildDirectory("scaffold");
        doReturn(resourcesFacet).when(project).getFacet(anyObject());
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Speaker.java"));
        EntityConfig entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
        assertSpeakerEntityConfig(entityConfig);
        entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Talk.java"));
        entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
        assertTalkEntityConfig(entityConfig);
    }

    @Test
    public void shouldCreateEntityConfigWithCustomGlobalConfig() {
        JavaSourceFacet sourceFacet = mock(JavaSourceFacet.class);
        String sourceDirPath = Paths.get("target/test-classes").toAbsolutePath().toString();
        DirectoryResource sourceDir = mock(DirectoryResource.class);
        doReturn(sourceDirPath).when(sourceDir).getFullyQualifiedName();
        doReturn(sourceDir).when(sourceFacet).getSourceDirectory();
        FileResource<?> entityConfigFile = mock(FileResource.class);
        doReturn(false).when(entityConfigFile).exists();
        doReturn(null).when(entityConfigFile).setContents(anyString());
        DirectoryResource scaffoldDir = mock(DirectoryResource.class);
        doReturn(entityConfigFile).when(scaffoldDir).getChild(anyString());
        FileResource<?> globalConfigFile = mock(FileResource.class);
        doReturn(globalConfigFile).when(scaffoldDir).getChild("global-config.yml");
        InputStream is = ScaffoldConfigLoader.class.getResourceAsStream("/scaffold/custom-global-config.yml");
        doReturn(is).when(globalConfigFile).getResourceInputStream();
        doReturn(sourceFacet).when(project).getFacet(JavaSourceFacet.class);
        ResourcesFacet resourcesFacet = mock(ResourcesFacet.class);
        DirectoryResource directoryResource = mock(DirectoryResource.class);
        doReturn(directoryResource).when(resourcesFacet).getResourceDirectory();
        doReturn(scaffoldDir).when(directoryResource).getChildDirectory("scaffold");
        doReturn(resourcesFacet).when(project).getFacet(ResourcesFacet.class);
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Speaker.java"));
        EntityConfig entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
        assertThat(entityConfig).isNotNull()
            .extracting("displayField", "menuIcon").contains("firstname", "fa fa-circle");
        assertThat(entityConfig.getFields()).isNotNull().hasSize(11);

        assertThat(entityConfig.getFieldConfigByName("id"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 30, true, false);

        assertThat(entityConfig.getFieldConfigByName("version"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 30, false, false);

        assertThat(entityConfig.getFieldConfigByName("firstname"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 30, true, false);

        assertThat(entityConfig.getFieldConfigByName("surname"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 30, true, false);

        assertThat(entityConfig.getFieldConfigByName("bio"))
            .extracting("type", "length", "required", "hidden")
            .contains(TEXT_AREA, 2000, false, false);

        assertThat(entityConfig.getFieldConfigByName("twitter"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 30, false, false);

        assertThat(entityConfig.getFieldConfigByName("talks"))
            .extracting("type", "length", "required", "hidden")
            .contains(SELECT_MANY_MENU, 30, false, false);

        assertThat(entityConfig.getFieldConfigByName("id"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 30, true, false);

        assertThat(entityConfig.getFieldConfigByName("street"))
            .extracting("type", "length", "required", "hidden")
            .contains(TEXT_AREA, 50, false, false);

        assertThat(entityConfig.getFieldConfigByName("city"))
            .extracting("type", "length", "required", "hidden")
            .contains(TEXT_AREA, 50, false, false);

        assertThat(entityConfig.getFieldConfigByName("zipcode"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 10, false, false);

        assertThat(entityConfig.getFieldConfigByName("state"))
            .extracting("type", "length", "hidden")
            .contains(INPUT_TEXT, 30, false, false);

        assertThat(entityConfig.getDatatableEditable()).isTrue();

        entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Talk.java"));
        entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);

        assertThat(entityConfig).isNotNull()
            .extracting("displayField", "menuIcon").contains("title", "fa fa-circle");
        assertThat(entityConfig.getFields()).isNotNull().hasSize(7);

        assertThat(entityConfig.getFieldConfigByName("id"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 30, true, false);

        assertThat(entityConfig.getFieldConfigByName("version"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 30, false, false);

        assertThat(entityConfig.getFieldConfigByName("title"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 30, true, false);

        assertThat(entityConfig.getFieldConfigByName("room"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 30, true, false);

        assertThat(entityConfig.getFieldConfigByName("description"))
            .extracting("type", "length", "required", "hidden")
            .contains(TEXT_AREA, 2000, false, false);

        assertThat(entityConfig.getFieldConfigByName("date"))
            .extracting("type", "length", "required", "hidden")
            .contains(CALENDAR, 30, false, false);

        assertThat(entityConfig.getFieldConfigByName("speaker"))
            .extracting("type", "length", "required", "hidden")
            .contains(SELECT_ONE_MENU, 30, false, false);

        assertThat(entityConfig.getDatatableEditable()).isTrue();
    }

    private void assertSpeakerEntityConfig(EntityConfig entityConfig) {
        assertThat(entityConfig).isNotNull()
            .extracting("displayField", "menuIcon").contains("firstname", "fa fa-circle-o");
        assertThat(entityConfig.getFields()).isNotNull().hasSize(11);

        assertThat(entityConfig.getFieldConfigByName("id"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 100, true, false);

        assertThat(entityConfig.getFieldConfigByName("version"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 100, false, false);

        assertThat(entityConfig.getFieldConfigByName("firstname"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 100, true, false);

        assertThat(entityConfig.getFieldConfigByName("surname"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 100, true, false);

        assertThat(entityConfig.getFieldConfigByName("bio"))
            .extracting("type", "length", "required", "hidden")
            .contains(TEXT_AREA, 2000, false, false);

        assertThat(entityConfig.getFieldConfigByName("twitter"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 100, false, false);

        assertThat(entityConfig.getFieldConfigByName("talks"))
            .extracting("type", "length", "required", "hidden")
            .contains(CHECKBOXMENU, 100, false, false);

        assertThat(entityConfig.getFieldConfigByName("street"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 50, false, false);

        assertThat(entityConfig.getFieldConfigByName("city"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 50, false, false);

        assertThat(entityConfig.getFieldConfigByName("zipcode"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 10, false, false);

        assertThat(entityConfig.getFieldConfigByName("state"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 100, false, false);

        assertThat(entityConfig.getDatatableEditable()).isFalse();
    }

    private void assertTalkEntityConfig(EntityConfig entityConfig) {
        assertThat(entityConfig).isNotNull()
            .extracting("displayField", "menuIcon").contains("title", "fa fa-circle-o");
        assertThat(entityConfig.getFields()).isNotNull().hasSize(7);

        assertThat(entityConfig.getFieldConfigByName("id"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 100, true, false);

        assertThat(entityConfig.getFieldConfigByName("version"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 100, false, false);

        assertThat(entityConfig.getFieldConfigByName("title"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 100, true, false);

        assertThat(entityConfig.getFieldConfigByName("room"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 100, true, false);

        assertThat(entityConfig.getFieldConfigByName("description"))
            .extracting("type", "length", "required", "hidden")
            .contains(TEXT_AREA, 2000, false, false);

        assertThat(entityConfig.getFieldConfigByName("date"))
            .extracting("type", "length", "required", "hidden")
            .contains(CALENDAR, 100, false, false);

        assertThat(entityConfig.getFieldConfigByName("speaker"))
            .extracting("type", "length", "required", "hidden")
            .contains(AUTOCOMPLETE, 100, false, false);

        assertThat(entityConfig.getDatatableEditable()).isFalse();
    }
}
