package com.github.adminfaces.addon.util;

public interface Constants {
    
    public static final String NEW_LINE = System.getProperty("line.separator");

    class Packages {

        public static final String BEAN = "bean";

        public static final String SERVICE = "service";

        public static final String MODEL = "model";

		public static final String REPOSITORY = "repository";
    }

    class WebResources {

        public static final String PAGE_TEMPLATE = "#{layoutMB.template}";

        public static final String INDEX_PAGE = "/index.xhtml";

        public static final String INCLUDES = "/includes";

        public static final String TEMPLATES = "/WEB-INF/templates";

        public static final String TEMPLATE_DEFAULT = TEMPLATES + "/template.xhtml";

        public static final String TEMPLATE_TOP = TEMPLATES + "/template-top.xhtml";
        
        public static final String LEFT_MENU = INCLUDES + "/menu.xhtml";
        
        public static final String TOP_MENU = INCLUDES + "/menubar.xhtml";

        public static final String INDEX_HTML = "/index.html";

        public static final String LOGIN_PAGE = "/login.xhtml";

        public static final String SCAFFOLD_RESOURCES = "/scaffold";
    }
}
