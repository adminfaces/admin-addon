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
package com.github.adminfaces.addon;

import com.github.adminfaces.addon.freemarker.FreemarkerTemplateProcessor;
import com.github.adminfaces.addon.freemarker.GenerateDataSetValueFromField;
import com.github.adminfaces.addon.freemarker.TemplateFactory;
import com.github.adminfaces.addon.scaffold.config.ScaffoldConfigLoader;
import com.github.adminfaces.addon.scaffold.model.EntityConfig;
import com.github.adminfaces.addon.scaffold.model.ScaffoldEntity;
import static com.github.adminfaces.addon.util.AdminScaffoldUtils.extractEntityRequiredFields;
import static com.github.adminfaces.addon.util.AdminScaffoldUtils.resolveToManyAssociationFields;
import static com.github.adminfaces.addon.util.AdminScaffoldUtils.resolveToOneAssociationFields;
import freemarker.template.Template;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.api.AssertionsForClassTypes;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.metawidget.util.simple.StringUtils;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 *
 * @author rmpestano
 */
@RunWith(JUnit4.class)
public class FreemarkerTemplatesTest {

    private static TemplateFactory templateFactory;

    private Project project = mock(Project.class);

    @BeforeClass
    public static void init() {
        templateFactory = new TemplateFactory();
    }

    @Before
    public void tearUp() {
        FileResource<?> entityConfigFile = mock(FileResource.class);
        doReturn(false).when(entityConfigFile).exists();
        doReturn(null).when(entityConfigFile).setContents(anyString());
        DirectoryResource scaffoldDir = mock(DirectoryResource.class);
        doReturn(entityConfigFile).when(scaffoldDir).getChild(anyString());
        FileResource<?> globalConfigFile = mock(FileResource.class);
        doReturn(globalConfigFile).when(scaffoldDir).getChild("global-config.yml");
        InputStream is = ScaffoldConfigLoader.class.getResourceAsStream("/scaffold/global-config.yml");
        doReturn(is).when(globalConfigFile).getResourceInputStream();
        ResourcesFacet resourcesFacet = mock(ResourcesFacet.class);
        DirectoryResource directoryResource = mock(DirectoryResource.class);
        doReturn(directoryResource).when(resourcesFacet).getResourceDirectory();
        doReturn(scaffoldDir).when(directoryResource).getChildDirectory("scaffold");
        doReturn(resourcesFacet).when(project).getFacet(ResourcesFacet.class);
    }

    @Test
    public void shouldProcessServiceTestTemplate() {
        Template serviceTestTemplate = templateFactory.getServiceTestTemplate();
        Assert.assertNotNull(serviceTestTemplate);
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, getClass().getResourceAsStream("/com/github/adminfaces/addon/model/SimpleEntity.java"));
        JavaClassSource service = Roaster.parse(JavaClassSource.class, getClass().getResourceAsStream("/com/github/adminfaces/addon/service/SimpleService.java"));
        EntityConfig entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
        ScaffoldEntity scaffoldEntity = new ScaffoldEntity(entity, entityConfig, project);
        String ccEntity = StringUtils.decapitalize(scaffoldEntity.getName());
        String ccService = StringUtils.decapitalize(service.getName());
        Map<Object, Object> context = new HashMap<>();
        context.put("entity", scaffoldEntity);
        context.put("service", service);
        context.put("ccService", ccService);
        context.put("ccEntity", ccEntity);
        context.put("requiredFields", extractEntityRequiredFields(scaffoldEntity, project));
        context.put("toManyFields", resolveToManyAssociationFields(scaffoldEntity.getFields()));
        context.put("toOneFields", resolveToOneAssociationFields(scaffoldEntity.getFields()));
        String result = FreemarkerTemplateProcessor.processTemplate(context, serviceTestTemplate);
        AssertionsForClassTypes.assertThat(result).contains("package com.github.adminfaces.starter.bean;\n" +
"\n" +
"import Person;\n" +
"import com.github.adminfaces.addon.service.SimpleService;\n" +
"import com.github.adminfaces.addon.model.Speaker;  \n" +
"import com.github.database.rider.cdi.api.DBUnitInterceptor;\n" +
"import com.github.database.rider.core.api.dataset.DataSet;\n" +
"import javax.inject.Inject;\n" +
"import org.apache.deltaspike.jpa.api.transaction.Transactional;\n" +
"import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;\n" +
"import static org.assertj.core.api.Assertions.assertThat;\n" +
"import java.util.*;\n" +
"import org.junit.Test;\n" +
"import org.junit.runner.RunWith;\n" +
" \n" +
"@RunWith(CdiTestRunner.class)\n" +
"@DBUnitInterceptor\n" +
"@Transactional\n" +
"public class SimpleServiceIt {\n" +
"\n" +
"    @Inject\n" +
"    SimpleService simpleService;\n" +
"\n" +
"    @Test\n" +
"    @DataSet(value=\"person.yml\")\n" +
"    public void shouldFindPerson() {\n" +
"        Person person = simpleService.findById(-1);\n" +
"        assertThat(person).isNotNull();\n" +
"    } \n" +
"\n" +
"    @Test\n" +
"    @DataSet(cleanBefore = true, disableConstraints = true)\n" +
"    public void shouldInsertPerson() {\n" +
"        Person person = new Person();\n" +
"        person.setAge(new Random().nextInt());\n" +
"        person.setFirstname(UUID.randomUUID().toString());\n" +
"        Speaker speaker = new Speaker();\n" +
"        person.setSpeaker(speaker);\n" +
"        Person savedPerson = simpleService.saveOrUpdate(person);\n" +
"        assertThat(savedPerson.getId()).isNotNull();\n" +
"    } \n" +
"\n" +
"    @Test\n" +
"    @DataSet(value=\"person.yml\")\n" +
"    public void shouldRemovePerson() {\n" +
"        assertThat(simpleService.count()).isEqualTo(1L);\n" +
"        Person person = simpleService.findById(-1);\n" +
"        assertThat(person).isNotNull();\n" +
"        simpleService.remove(person);\n" +
"        assertThat(simpleService.count()).isEqualTo(0L);\n" +
"    }\n" +
"\n" +
"    @Test\n" +
"    @DataSet(value=\"person.yml\", disableConstraints = true)\n" +
"    public void shouldUpdatePerson() {\n" +
"        Person person = simpleService.findById(-1);\n" +
"        assertThat(person).isNotNull();\n" +
"        Integer age = new Random().nextInt();\n" +
"        person.setAge(age);\n" +
"        String firstname = UUID.randomUUID().toString();\n" +
"        person.setFirstname(firstname);\n" +
"        person = simpleService.saveOrUpdate(person);\n" +
"        assertThat(person.getAge()).isEqualTo(age);\n" +
"        assertThat(person.getFirstname()).isEqualTo(firstname);\n" +
"    }\n" +
"\n" +
"}");
    }
    
    @Test
    public void shouldProcessDataSetTemplate() {
        Template datasetTemplate = templateFactory.getDataSetTemplate();
        Assert.assertNotNull(datasetTemplate);
        JavaClassSource entity = Roaster.parse(JavaClassSource.class, getClass().getResourceAsStream("/com/github/adminfaces/addon/model/SimpleEntity.java"));
        JavaClassSource service = Roaster.parse(JavaClassSource.class, getClass().getResourceAsStream("/com/github/adminfaces/addon/service/SimpleService.java"));
        EntityConfig entityConfig = ScaffoldConfigLoader.createOrLoadEntityConfig(entity, project);
        ScaffoldEntity scaffoldEntity = new ScaffoldEntity(entity, entityConfig, project);
        String ccEntity = StringUtils.decapitalize(scaffoldEntity.getName());
        String ccService = StringUtils.decapitalize(service.getName());
        Map<Object, Object> context = new HashMap<>();
        context.put("entity", scaffoldEntity);
        context.put("service", service);
        context.put("ccService", ccService);
        context.put("ccEntity", ccEntity);
        context.put("requiredFields", extractEntityRequiredFields(scaffoldEntity,project));
        context.put("datasetValue", new GenerateDataSetValueFromField());
        context.put("toManyFields", resolveToManyAssociationFields(scaffoldEntity.getFields()));
        context.put("toOneFields", resolveToOneAssociationFields(scaffoldEntity.getFields()));
        String result = FreemarkerTemplateProcessor.processTemplate(context, datasetTemplate);
        AssertionsForClassTypes.assertThat(result).startsWith("Person:\n" +
"  id: -1");
    }

}
