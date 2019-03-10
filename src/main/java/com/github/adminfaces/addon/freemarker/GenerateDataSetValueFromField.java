/*
 * The MIT License
 *
 * Copyright 2019 rafael-pestano.
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
package com.github.adminfaces.addon.freemarker;

import freemarker.ext.beans.StringModel;
import freemarker.template.SimpleDate;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.FieldSource;
import org.jboss.forge.roaster.model.source.JavaClassSource;

import javax.persistence.Column;

/**
 * @author rafael-pestano
 */
public class GenerateDataSetValueFromField implements TemplateMethodModelEx {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-dd-MM");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-dd-MM");


    @Override
    public Object exec(List list) throws TemplateModelException {
        FieldSource<JavaClassSource> field = (FieldSource<JavaClassSource>) ((StringModel) list.get(0)).getWrappedObject();
        if (field.getType().getSimpleName().equals("String")) {
            int length = resolveLengthAttribute(field, 10);
            String value = UUID.randomUUID().toString();
            return value.length() > length ? value.substring(0, length) : value;
        } else if (field.getType().getSimpleName().equals("Integer") || field.getType().getQualifiedName().equals("int")) {
            return new Random().nextInt()+"";
        } else if (field.getType().getSimpleName().equals("Long") || field.getType().getQualifiedName().equals("long")) {
            return new Random().nextLong()+"";
        } else if (field.getType().getSimpleName().equals("Double") || field.getType().getQualifiedName().equals("double")) {
            return new Random().nextDouble()+"";
        } else if (field.getType().getSimpleName().equals("Float") || field.getType().getQualifiedName().equals("float")) {
            return new Random().nextFloat()+"";
        } else if (field.getType().getSimpleName().equals("Short") || field.getType().getQualifiedName().equals("short")) {
            return (short)42+"";    
        } else if (field.getType().getSimpleName().equals("Calendar")) {
            return SDF.format(Calendar.getInstance().getTime());
        } else if (field.getType().getSimpleName().equals("Date")) {
            return SDF.format(new Date());
        } else if (field.getType().getSimpleName().equals("LocalDate")) {
            return FORMATTER.format(java.time.LocalDate.now());
        } else if (field.getType().getSimpleName().equals("LocalDateTime")) {
            return FORMATTER.format(java.time.LocalDateTime.now());
        }

        return field.getType().isParameterized() ? field.getType().getTypeArguments().get(0) : field.getType();
    }

    private static Integer resolveLengthAttribute(FieldSource<JavaClassSource> field, Integer defaultValue) {
        AnnotationSource<JavaClassSource> columnAnnotation = field.getAnnotation(Column.class);
        Integer length = defaultValue;
        if (columnAnnotation != null && columnAnnotation.getStringValue("length") != null) {
            length = Integer.parseInt(columnAnnotation.getStringValue("length"));
        }
        return length;
    }
}
