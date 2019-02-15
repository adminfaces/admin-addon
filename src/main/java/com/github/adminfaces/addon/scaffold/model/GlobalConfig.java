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

/**
 * Scaffold addon default behaviour. It can be overriden by EntityConfig. 
 * In fact global config will be used to create entity config the first time.
 * @author rmpestano
 */
public class GlobalConfig {
    
    private ComponentTypeEnum toOneComponentType;
    private ComponentTypeEnum toManyComponentType;
    private ComponentTypeEnum dateComponentType;
    private Boolean datatableEditable;
    private Integer inputSize;
    private String menuIcon;

    public ComponentTypeEnum getToOneComponentType() {
        return toOneComponentType;
    }

    public void setToOneComponentType(ComponentTypeEnum toOneComponentType) {
        this.toOneComponentType = toOneComponentType;
    }

    public ComponentTypeEnum getToManyComponentType() {
        return toManyComponentType;
    }

    public void setToManyComponentType(ComponentTypeEnum toManyComponentType) {
        this.toManyComponentType = toManyComponentType;
    }

    public ComponentTypeEnum getDateComponentType() {
        return dateComponentType;
    }

    public void setDateComponentType(ComponentTypeEnum dateComponentType) {
        this.dateComponentType = dateComponentType;
    }

    public Boolean getDatatableEditable() {
        return datatableEditable;
    }

    public void setDatatableEditable(Boolean datatableEditable) {
        this.datatableEditable = datatableEditable;
    }

    public Integer getInputSize() {
        return inputSize;
    }

    public void setInputSize(Integer inputSize) {
        this.inputSize = inputSize;
    }

    public String getMenuIcon() {
        return menuIcon;
    }

    public void setMenuIcon(String menuIcon) {
        this.menuIcon = menuIcon;
    }
}
