package com.github.adminfaces.addon.freemarker;

import javax.annotation.PostConstruct;

import freemarker.template.Template;
import java.io.Serializable;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TemplateFactory implements Serializable {

	private static final String INDEX_TEMPLATE = "scaffold/freemarker/index.xhtml";
	
	private static final String LOGIN_TEMPLATE = "scaffold/freemarker/login.xhtml";

	private static final String TEMPLATE_TOP_TEMPLATE = "scaffold/freemarker/template-top.xhtml";

	private static final String TEMPLATE_DEFAULT_TEMPLATE = "scaffold/freemarker/template.xhtml";

	private Template indexTemplate;

	private Template loginTemplate;

	private Template templateTop;

	private Template templateDefault;

	@PostConstruct
	public void loadTemplates() {
		indexTemplate = FreemarkerTemplateProcessor.getTemplate(INDEX_TEMPLATE);
		loginTemplate = FreemarkerTemplateProcessor.getTemplate(LOGIN_TEMPLATE);
		templateDefault = FreemarkerTemplateProcessor.getTemplate(TEMPLATE_DEFAULT_TEMPLATE);
		templateTop = FreemarkerTemplateProcessor.getTemplate(TEMPLATE_TOP_TEMPLATE);
	}

	public Template getIndexTemplate() {
		return indexTemplate;
	}

	public Template getTemplateTop() {
		return templateTop;
	}

	public Template getTemplateDefault() {
		return templateDefault;
	}
	
	public Template getLoginTemplate() {
    return loginTemplate;
  }
	
	

}
