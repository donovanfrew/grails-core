/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.servlet.view;

import grails.util.GrailsUtil;
import groovy.lang.GroovyObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.plugins.GrailsPluginManager;
import org.codehaus.groovy.grails.plugins.PluginManagerAware;
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.pages.discovery.GrailsConventionGroovyPageLocator;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageCompiledScriptSource;
import org.codehaus.groovy.grails.web.pages.discovery.GroovyPageScriptSource;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.codehaus.groovy.grails.web.util.WebUtils;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractUrlBasedView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates the existance of a view for different extensions choosing which one to delegate to.
 *
 * @author Graeme Rocher
 * @since 0.1
 */
public class GrailsViewResolver extends InternalResourceViewResolver implements ApplicationContextAware, PluginManagerAware, GrailsApplicationAware {

    private String localPrefix;
    private static final Log LOG = LogFactory.getLog(GrailsViewResolver.class);

    public static final String GSP_SUFFIX = ".gsp";
    public static final String JSP_SUFFIX = ".jsp";

    protected GroovyPagesTemplateEngine templateEngine;
    protected GrailsConventionGroovyPageLocator groovyPageLocator;

    // no need for static cache since GrailsViewResolver is in app context
    private Map<String, View> VIEW_CACHE = new ConcurrentHashMap<String, View>();
    private static final char DOT = '.';
    private GrailsApplication grailsApplication;

    /**
     * Constructor.
     */
    public GrailsViewResolver() {
        setCache(false);
    }

    public void setGroovyPageLocator(GrailsConventionGroovyPageLocator groovyPageLocator) {
        this.groovyPageLocator = groovyPageLocator;
    }

    @Override
    protected View loadView(String viewName, Locale locale) throws Exception {
        Assert.notNull(templateEngine, "Property [templateEngine] cannot be null");

        if (GrailsUtil.isDevelopmentEnv()) {
            return createGrailsView(viewName);
        }

        View view = VIEW_CACHE.get(viewName);
        if (view == null || (templateEngine.isReloadEnabled() && view instanceof GroovyPageView && ((GroovyPageView)view).isExpired())) {
            view = createGrailsView(viewName);
        }
        VIEW_CACHE.put(viewName, view);
        return view;
    }

    private View createGrailsView(String viewName) throws Exception {
        // try GSP if res is null

        GrailsWebRequest webRequest = WebUtils.retrieveGrailsWebRequest();

        HttpServletRequest request = webRequest.getCurrentRequest();
        GroovyObject controller = webRequest.getAttributes().getController(request);

        if (grailsApplication == null) {

            grailsApplication = getApplicationContext().getBean(GrailsApplication.APPLICATION_ID, GrailsApplication.class);
        }


        GroovyPageScriptSource scriptSource = null;
        if(controller == null) {
            scriptSource = groovyPageLocator.findViewByPath(viewName);
        }
        else {
            scriptSource = groovyPageLocator.findView(controller, viewName);
        }
        if (scriptSource != null) {
            if(scriptSource instanceof GroovyPageCompiledScriptSource) {
                GroovyPageCompiledScriptSource compiled = (GroovyPageCompiledScriptSource) scriptSource;
                return createGroovyPageView(webRequest, compiled.getURI(), compiled.getCompiledClass());
            }
            else {
                return createGroovyPageView(webRequest, scriptSource.getURI());
            }
        }

        AbstractUrlBasedView view = buildView(viewName);
        view.setApplicationContext(getApplicationContext());
        view.afterPropertiesSet();
        return view;
    }

    private View createGroovyPageView(GrailsWebRequest webRequest, String gspView, Class<?> viewClass) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolved GSP view at URI [" + gspView + "]");
        }
        GroovyPageView gspSpringView = new GroovyPageView();
        gspSpringView.setServletContext(webRequest.getServletContext());
        gspSpringView.setViewClass(viewClass);
        gspSpringView.setApplicationContext(getApplicationContext());
        gspSpringView.setTemplateEngine(templateEngine);
        return gspSpringView;
    }

    private View createGroovyPageView(GrailsWebRequest webRequest, String gspView) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Resolved GSP view at URI [" + gspView + "]");
        }
        GroovyPageView gspSpringView = new GroovyPageView();
        gspSpringView.setServletContext(webRequest.getServletContext());
        gspSpringView.setUrl(gspView);
        gspSpringView.setApplicationContext(getApplicationContext());
        gspSpringView.setTemplateEngine(templateEngine);
        return gspSpringView;
    }

    public void setPluginManager(GrailsPluginManager pluginManager) {
        // ignored, here for compatibility
    }

    @Override
    public void setPrefix(String prefix) {
        super.setPrefix(prefix);
        this.localPrefix = prefix;
    }

    public void setTemplateEngine(GroovyPagesTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void setGrailsApplication(GrailsApplication grailsApplication) {
        this.grailsApplication = grailsApplication;
    }
}
