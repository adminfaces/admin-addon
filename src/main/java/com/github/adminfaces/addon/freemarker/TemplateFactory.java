package com.github.adminfaces.addon.freemarker;

import java.io.Serializable;

import freemarker.template.Template;

public class TemplateFactory implements Serializable {

    private static final String INDEX_TEMPLATE = "scaffold/freemarker/index.xhtml";

    private static final String LOGIN_TEMPLATE = "scaffold/freemarker/login.xhtml";

    private static final String TEMPLATE_TOP_TEMPLATE = "scaffold/freemarker/template-top.xhtml";

    private static final String TEMPLATE_DEFAULT_TEMPLATE = "scaffold/freemarker/template.xhtml";

    private static final String SERVICE_TEMPLATE = "scaffold/freemarker/Service.jv";

    private static final String REPOSITORY_TEMPLATE = "scaffold/freemarker/Repository.jv";

    private static final String LIST_MB_TEMPLATE = "scaffold/freemarker/ListMB.jv";

    private static final String FORM_MB_TEMPLATE = "scaffold/freemarker/FormMB.jv";
    
    private static final String LIST_PAGE_TEMPLATE = "scaffold/freemarker/list.xhtml";
    
    private static final String FORM_PAGE_TEMPLATE = "scaffold/freemarker/form.xhtml";

    private static final String DATASET_TEMPLATE = "scaffold/freemarker/dataset.yml";

    private static final String SERVICE_TEST_TEMPLATE = "scaffold/freemarker/ServiceTest.jv";

    private Template indexTemplate;

    private Template loginTemplate;

    private Template templateTop;

    private Template templateDefault;

    private Template serviceTemplate;

    private Template repositoryTemplate;

    private Template listMBTemplate;

    private Template formMBTemplate;
    
    private Template listPageTemplate;
    
    private Template formPageTemplate;

    private Template dataSetTemplate;

    private Template serviceTestTemplate;


    public Template getIndexTemplate() {
        if(indexTemplate == null) {
            indexTemplate = FreemarkerTemplateProcessor.getTemplate(INDEX_TEMPLATE);
        }
        return indexTemplate;
    }

    public Template getTemplateTop() {
        if(templateTop == null) {
            templateTop = FreemarkerTemplateProcessor.getTemplate(TEMPLATE_TOP_TEMPLATE);
        }
        return templateTop;
    }

    public Template getTemplateDefault() {
        if(templateDefault == null) {
            templateDefault = FreemarkerTemplateProcessor.getTemplate(TEMPLATE_DEFAULT_TEMPLATE);
        }
        return templateDefault;
    }

    public Template getLoginTemplate() {
        if(loginTemplate == null) {
            loginTemplate = FreemarkerTemplateProcessor.getTemplate(LOGIN_TEMPLATE);
        }
        return loginTemplate;
    }

    public Template getServiceTemplate() {
        if(serviceTemplate == null) {
            serviceTemplate = FreemarkerTemplateProcessor.getTemplate(SERVICE_TEMPLATE);
        }
        return serviceTemplate;
    }

    public Template getRepositoryTemplate() {
        if(repositoryTemplate == null) {
            repositoryTemplate = FreemarkerTemplateProcessor.getTemplate(REPOSITORY_TEMPLATE);
        }
        return repositoryTemplate;
    }

    public Template getListMBTemplate() {
        if(listMBTemplate == null) {
            listMBTemplate = FreemarkerTemplateProcessor.getTemplate(LIST_MB_TEMPLATE);
        }
        return listMBTemplate;
    }

    public Template getFormMBTemplate() {
        if(formMBTemplate == null) {
            formMBTemplate = FreemarkerTemplateProcessor.getTemplate(FORM_MB_TEMPLATE);
        }
        return formMBTemplate;
    }

    public Template getListPageTemplate() {
        if(listPageTemplate == null) {
            listPageTemplate = FreemarkerTemplateProcessor.getTemplate(LIST_PAGE_TEMPLATE);
        }
        return listPageTemplate;
    }

    public Template getFormPageTemplate() {
        if(formPageTemplate == null) {
            formPageTemplate = FreemarkerTemplateProcessor.getTemplate(FORM_PAGE_TEMPLATE);
        }
        return formPageTemplate;
    }

    public Template getDataSetTemplate() {
        if(dataSetTemplate == null) {
            dataSetTemplate = FreemarkerTemplateProcessor.getTemplate(DATASET_TEMPLATE);
        }
        return dataSetTemplate;
    }

    public Template getServiceTestTemplate() {
        if(serviceTestTemplate == null) {
            serviceTestTemplate = FreemarkerTemplateProcessor.getTemplate(SERVICE_TEST_TEMPLATE);
        }
        return serviceTestTemplate;
    }
}
