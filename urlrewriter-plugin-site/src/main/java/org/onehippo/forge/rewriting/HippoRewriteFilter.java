/**
 * Copyright (C) 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.rewriting;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tools.ant.filters.StringInputStream;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.forge.rewriting.repo.RewritingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.Status;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;
import org.tuckey.web.filters.urlrewrite.UrlRewriteWrappedResponse;
import org.tuckey.web.filters.urlrewrite.UrlRewriter;
import org.tuckey.web.filters.urlrewrite.utils.Log;
import org.tuckey.web.filters.urlrewrite.utils.ModRewriteConfLoader;
import org.tuckey.web.filters.urlrewrite.utils.ServerNameMatcher;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * Hippo URL Rewrite Filter based on Url Rewrite Filter originally written by Paul Tuckey.
 * @see <a href="http://www.tuckey.org/urlrewrite/">Tucky Url Rewrite Filter</a>
 * @see <a href="http://http://urlrewriter.forge.onehippo.org/">Hippo Rewrite Filter project page on forge</a>
 * @version $Id$
 */
public class HippoRewriteFilter extends UrlRewriteFilter {

    private static Logger log = LoggerFactory.getLogger(HippoRewriteFilter.class);

    private final Object lock = new Object();

    // TODO check if this is needed
    private static final String DEFAULT_STATUS_ENABLED_ON_HOSTS = "localhost, local, 127.0.0.1";

    //
    private ServerNameMatcher statusServerNameMatcher;
    private UrlRewriter urlRewriter;
    private boolean statusEnabled = true;
    private String statusPath = "/rewrite-status";
    private List<String> disabledRuleTypes = new ArrayList<String>();
    private String rulesLocation = null;

    private Conf confLastLoaded;

    // hippo vars
    private ServletContext context;
    private volatile boolean initialized;

    private RewritingManager rewritingManager;


    public void init(final FilterConfig filterConfig) throws ServletException {

        if (filterConfig == null) {
            log.error("unable to init filter as filter config is null");
            return;
        }

        log.debug("init: calling destroy just in case we are being re-inited uncleanly");
        destroyActual();

        context = filterConfig.getServletContext();
        if (context == null) {
            log.error("unable to init as servlet context is null");
            return;
        }

        // set the conf of the logger to make sure we get the messages in context log
        Log.setConfiguration(filterConfig);

        // status enabled (default true)
        String statusEnabledConf = filterConfig.getInitParameter("statusEnabled");
        if (statusEnabledConf != null && !"".equals(statusEnabledConf)) {
            log.debug("statusEnabledConf set to " + statusEnabledConf);
            statusEnabled = "true".equals(statusEnabledConf.toLowerCase());
        }
        if (statusEnabled) {
            // status path (default /rewrite-status)
            String statusPathConf = filterConfig.getInitParameter("statusPath");
            if (statusPathConf != null && !"".equals(statusPathConf)) {
                statusPath = statusPathConf.trim();
                log.info("status display enabled, path set to " + statusPath);
            }
        } else {
            log.info("status display disabled");
        }

        String statusEnabledOnHosts = filterConfig.getInitParameter("statusEnabledOnHosts");
        if (StringUtils.isBlank(statusEnabledOnHosts)) {
            statusEnabledOnHosts = DEFAULT_STATUS_ENABLED_ON_HOSTS;
        } else {
            log.debug("statusEnabledOnHosts set to " + statusEnabledOnHosts);
        }
        statusServerNameMatcher = new ServerNameMatcher(statusEnabledOnHosts);

        //Specified disabled types
        if(!StringUtils.isBlank(filterConfig.getInitParameter("disabledRuleTypes"))){
            disabledRuleTypes = Arrays.asList(filterConfig.getInitParameter("disabledRuleTypes").split("\\s*,\\s*"));
        }

        //Rewrite rules locations
        rulesLocation = filterConfig.getInitParameter("rulesLocation");

        // now load conf from snippet in web.xml if modRewriteStyleConf is set
        String modRewriteConfText = filterConfig.getInitParameter("modRewriteConfText");
        if (!StringUtils.isBlank(modRewriteConfText)) {
            ModRewriteConfLoader loader = new ModRewriteConfLoader();
            Conf conf = new Conf();
            loader.process(modRewriteConfText, conf);
            conf.initialise();
        }
    }


    private void fetchRules(final ServletRequest request) {
        if (HstServices.isAvailable()) {
            initialized = true; // set to true. if component is not there it will probably never be...
            rewritingManager = HstServices.getComponentManager().getComponent("org.onehippo.forge.rewriting.repo.RewritingManager");

            if (rewritingManager == null) {
                return;
            }
            // TODO we can make this fine grained
            StringBuilder builder = rewritingManager.loadRules(context ,request, rulesLocation, disabledRuleTypes);
            Conf conf = new Conf(context, new StringInputStream(builder.toString()), "hippo-repository-", "hippo-repository-rewrite-rules", false);
            checkConfLocal(conf);
        }

    }

    private void checkConfLocal(final Conf conf) {
        if (log.isDebugEnabled()) {
            if (conf.getRules() != null) {
                log.debug("initialized with " + conf.getRules().size() + " rules");
            }
            log.debug("conf is " + (conf.isOk() ? "ok" : "NOT ok"));
        }
        confLastLoaded = conf;
        if (conf.isOk() && conf.isEngineEnabled()) {
            urlRewriter = new UrlRewriter(conf);
            log.info("loaded (conf ok)");

        } else {
            if (!conf.isOk()) {
                log.error("Conf failed to load");
            }
            if (!conf.isEngineEnabled()) {
                log.error("Engine explicitly disabled in conf"); // not really an error but we want ot to show in logs
            }
            if (urlRewriter != null) {
                log.error("unloading existing conf");
                urlRewriter = null;
            }
        }
    }

    /**
     * Destroy is called by the application server when it unloads this filter.
     */
    public void destroy() {
        log.info("destroy called");
        destroyActual();
    }

    public void destroyActual() {
        destroyUrlRewriter();
        context = null;
    }

    protected void destroyUrlRewriter() {
        if (urlRewriter != null) {
            urlRewriter.destroy();
            urlRewriter = null;
        }
    }

    /**
     * The main method called for each request that this filter is mapped for.
     *
     * @param request  the request to filter
     * @param response the response to filter
     * @param chain    the chain for the filtering
     * @throws java.io.IOException
     * @throws ServletException
     */
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        // check if we loaded from repository, otherwise do nothing
        if (!initialized) {
            synchronized (lock) {
                fetchRules(request);
            }
        }
        // check if we need to reload rules:
        if (needsReloading()) {
            fetchRules(request);
        }

        if (!initialized) {
            // continue:
            chain.doFilter(request, response);
            log.warn("############## hippo rewrite filter not initialized yet ###########");
            return;
        }

        UrlRewriter rewriter = getUrlRewriter(request, response, chain);
        final HttpServletRequest hsRequest = (HttpServletRequest) request;
        final HttpServletResponse hsResponse = (HttpServletResponse) response;
        HttpServletResponse urlRewriteWrappedResponse = new UrlRewriteWrappedResponse(hsResponse, hsRequest, rewriter);
        // check for status request
        if (statusEnabled && statusServerNameMatcher.isMatch(request.getServerName())) {
            String uri = hsRequest.getRequestURI();
            if (log.isDebugEnabled()) {
                log.debug("checking for status path on " + uri);
            }
            String contextPath = hsRequest.getContextPath();
            if (uri != null && uri.startsWith(contextPath + statusPath)) {
                showStatus(hsRequest, urlRewriteWrappedResponse);
                return;
            }
        }

        boolean requestRewritten = false;
        if (rewriter != null) {
            // process the request
            requestRewritten = rewriter.processRequest(hsRequest, urlRewriteWrappedResponse, chain);

        } else {
            if (log.isDebugEnabled()) {
                log.debug("urlRewriter engine not loaded ignoring request (could be a conf file problem)");
            }
        }
        // if no rewrite has taken place continue as normal
        if (!requestRewritten) {
            chain.doFilter(hsRequest, urlRewriteWrappedResponse);
        }
    }

    private boolean needsReloading() {
        return rewritingManager != null && rewritingManager.needReloading();
    }


    /**
     * Called for every request.
     * <p/>
     * Split from doFilter so that it can be overriden.
     */
    protected UrlRewriter getUrlRewriter(ServletRequest request, ServletResponse response, FilterChain chain) {
        // check to see if the conf needs reloading
        if (isTimeToReloadConf(request)) {
            reloadConf(request);
        }
        return urlRewriter;
    }

    /**
     * Is it time to reload the configuration now.  Depends on is conf reloading is enabled.
     *
     * @param request
     */
    public boolean isTimeToReloadConf(final ServletRequest request) {
        // forced reload:
        String rewrite = request.getParameter("reloadRewriteRules");
        if (rewrite != null && rewrite.equals("true")) {
            return true;
        }
        return needsReloading();
    }

    /**
     * Forcibly reload the configuration now.
     *
     * @param request
     */
    public void reloadConf(final ServletRequest request) {

    }


    /**
     * Show the status of the conf and the filter to the user.
     *
     * @param request  to get status info from
     * @param response response to show the status on.
     * @throws java.io.IOException if the output cannot be written
     */
    private void showStatus(final HttpServletRequest request, final ServletResponse response)
            throws IOException {

        log.debug("showing status");

        Status status = new Status(confLastLoaded, this);
        status.displayStatusInContainer(request);

        response.setContentType("text/html; charset=UTF-8");
        response.setContentLength(status.getBuffer().length());

        final PrintWriter out = response.getWriter();
        out.write(status.getBuffer().toString());
        out.close();

    }


    public boolean isStatusEnabled() {
        return statusEnabled;
    }

    public String getStatusPath() {
        return statusPath;
    }

    public boolean isLoaded() {
        return urlRewriter != null;
    }

    public boolean isConfReloadCheckEnabled() {

        // dummy
        return true;
    }

    public Date getConfReloadLastCheck() {
        if (rewritingManager != null) {
            return rewritingManager.getLastLoadDate();
        }
        return new Date();
    }

    public int getConfReloadCheckInterval() {
        // dummy
        return 100000;
    }

}
