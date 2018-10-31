/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.admin.addon.config;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AdminConfiguration  {

    private String projectName;


    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    
}
