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
package com.github.admin.addon.scaffold.model;

import static com.github.adminfaces.addon.scaffold.model.ComponentTypeEnum.*;
import com.github.adminfaces.addon.scaffold.model.EntityConfig;
import static org.mockito.Mockito.*;
import com.github.adminfaces.addon.scaffold.model.EntityConfigLoader;
import java.io.InputStream;
import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author rmpestano
 */
@RunWith(JUnit4.class)
public class EntityConfigLoaderTest {

    @Test
    public void shouldCreateEntityConfig() {
        FileResource<?> entityConfigFile = mock(FileResource.class);
        doReturn(false).when(entityConfigFile).exists();
        doReturn(null).when(entityConfigFile).setContents(anyString());
        DirectoryResource scaffoldDir = mock(DirectoryResource.class);
        doReturn(entityConfigFile).when(scaffoldDir).getChild(anyString());
        Project project = mock(Project.class);
        ResourcesFacet resourcesFacet = mock(ResourcesFacet.class);
        DirectoryResource directoryResource = mock(DirectoryResource.class);
        doReturn(directoryResource).when(resourcesFacet).getResourceDirectory();
        doReturn(scaffoldDir).when(directoryResource).getChildDirectory("scaffold");
        doReturn(resourcesFacet).when(project).getFacet(anyObject());
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, EntityConfigLoaderTest.class.getResourceAsStream("/com/github/admin/addon/model/Speaker.java"));
        EntityConfig entityConfig = EntityConfigLoader.createOrLoadEntityConfig(entity, project);
        assertEntityConfig(entityConfig);
    }

    @Test
    public void shouldLoadEntityConfig() {
        FileResource<?> entityConfigFile = mock(FileResource.class);
        doReturn(true).when(entityConfigFile).exists();
        doReturn(null).when(entityConfigFile).setContents(anyString());
        InputStream is = getClass().getResourceAsStream("/scaffold/speaker.yml");
        doReturn(is).when(entityConfigFile).getResourceInputStream();
        DirectoryResource scaffoldDir = mock(DirectoryResource.class);
        doReturn(entityConfigFile).when(scaffoldDir).getChild(anyString());
        Project project = mock(Project.class);
        ResourcesFacet resourcesFacet = mock(ResourcesFacet.class);
        DirectoryResource directoryResource = mock(DirectoryResource.class);
        doReturn(directoryResource).when(resourcesFacet).getResourceDirectory();
        doReturn(scaffoldDir).when(directoryResource).getChildDirectory("scaffold");
        doReturn(resourcesFacet).when(project).getFacet(anyObject());
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, EntityConfigLoaderTest.class.getResourceAsStream("/com/github/admin/addon/model/Speaker.java"));
        EntityConfig entityConfig = EntityConfigLoader.createOrLoadEntityConfig(entity, project);
        assertEntityConfig(entityConfig);
    }

    private void assertEntityConfig(EntityConfig entityConfig) {
        assertThat(entityConfig).isNotNull()
            .extracting("displayField", "menuIcon").contains("firstname", "fa fa-circle-o");
        assertThat(entityConfig.getFields()).isNotNull().hasSize(7);

        assertThat(entityConfig.getFieldConfigByName("id"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 50, true, false);

        assertThat(entityConfig.getFieldConfigByName("version"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 50, false, false);

        assertThat(entityConfig.getFieldConfigByName("firstname"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 50, true, false);

        assertThat(entityConfig.getFieldConfigByName("surname"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 50, true, false);

        assertThat(entityConfig.getFieldConfigByName("bio"))
            .extracting("type", "length", "required", "hidden")
            .contains(TEXT_AREA, 2000, false, false);

        assertThat(entityConfig.getFieldConfigByName("twitter"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_TEXT, 50, false, false);

        assertThat(entityConfig.getFieldConfigByName("talks"))
            .extracting("type", "length", "required", "hidden")
            .contains(CHECKBOXMENU, 50, false, false);

        assertThat(entityConfig.getFieldConfigByName("id"))
            .extracting("type", "length", "required", "hidden")
            .contains(INPUT_NUMBER, 50, true, false);
    }
}