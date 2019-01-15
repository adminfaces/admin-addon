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
package com.github.adminfaces.addon.freemarker.util;

import java.util.List;

import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 *
 * @author rmpestano
 */
public class ResolveEntityUIField implements TemplateMethodModelEx {

    @Override
    public Object exec(List list) throws TemplateModelException {
    	JavaClassSource entity = (JavaClassSource) ((StringModel)list.get(0)).getWrappedObject();
    	if(entity.getJavaDoc() != null && !entity.getJavaDoc().getTags("@ui-field").isEmpty()) {
    		return entity.getJavaDoc().getTags("@ui-field").get(0).getValue();
    	} else { //if there is no ui-field javadoc then look into entity fields
    		FieldSource<? extends JavaSource> fieldFound = null;
    		FieldSource<? extends JavaSource> idField = null;
    		for (FieldSource<? extends JavaSource> field : entity.getFields()) {
				if(field.hasAnnotation("javax.persistence.Column") && !isAssociation(field)) {
					if(field.hasAnnotation("javax.persistence.Id")) {
						idField = field;
					} else {
						fieldFound = field;
					}
				}
			}
    		if(fieldFound == null) {
    			fieldFound = idField;
    		}
    		return fieldFound.getName();
    	}
       
    }

	private boolean isAssociation(FieldSource<? extends JavaSource> field) {
		return field.hasAnnotation("javax.persistence.OneToMany") || field.hasAnnotation("javax.persistence.OneToOne") 
	            || field.hasAnnotation("javax.persistence.ManyToOne") || field.hasAnnotation("javax.persistence.ManyToMany");
	}

}
