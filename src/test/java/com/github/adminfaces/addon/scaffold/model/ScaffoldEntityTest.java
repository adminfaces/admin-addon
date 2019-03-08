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

import java.nio.file.Paths;
import static org.assertj.core.api.Assertions.assertThat;
import org.jboss.forge.addon.parser.java.facets.JavaSourceFacet;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 *
 * @author rmpestano
 */
@RunWith(JUnit4.class)
public class ScaffoldEntityTest {
    
    private Project project;
    
    @Before
    public void setup() {
        this.project = mock(Project.class);
        JavaSourceFacet javaSourceFacet = mock(JavaSourceFacet.class);
        DirectoryResource sourceDirectoryResource = mock(DirectoryResource.class);
        doReturn(Paths.get("").toAbsolutePath() +"/target/test-classes/").when(sourceDirectoryResource).getFullyQualifiedName();
        doReturn(sourceDirectoryResource).when(javaSourceFacet).getSourceDirectory();
        doReturn(javaSourceFacet).when(project).getFacet(JavaSourceFacet.class);
        
    }
     
    @Test
    public void shouldGetAssociationDisplayFieldFromToMany() {
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Speaker.java"));
        ScaffoldEntity scaffoldEntity = new ScaffoldEntity(entity, null, project);
        String associationDisplayField = scaffoldEntity.getAssociationDisplayField(entity.getField("talks"));
        assertThat(associationDisplayField).isNotEmpty().isNotBlank();
        assertThat(associationDisplayField).isEqualTo("title");
    }
    
     @Test
    public void shouldGetAssociationDisplayFieldFromToOne() {
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, ScaffoldConfigLoaderTest.class.getResourceAsStream("/com/github/adminfaces/addon/model/Talk.java"));
        ScaffoldEntity scaffoldEntity = new ScaffoldEntity(entity, null, project);
        String associationDisplayField = scaffoldEntity.getAssociationDisplayField(entity.getField("speaker"));
        assertThat(associationDisplayField).isNotEmpty().isNotBlank();
        assertThat(associationDisplayField).isEqualTo("firstname");
    }
    
}
