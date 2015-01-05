/**
 * Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.rewriting.repo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jcr.*;
import javax.jcr.observation.Event;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.forge.rewriting.UrlRewriteConstants;
import org.onehippo.forge.rewriting.UrlRewriteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 *
 */
public class RewritingManager {

    private static Logger log = LoggerFactory.getLogger(RewritingManager.class);

    // spring managed
    private String rewriteRulesLocation;
    private Repository repository;
    private Credentials credentials;
    private List<RewritingRulesExtractor> rewritingRulesExtractors;

    // configuration
    private boolean ignoreContextPath;
    private boolean skipPOST;
    private String[] skippedPrefixes = UrlRewriteConstants.SKIPPED_PREFIXES_DEFAULT_VALUE;

    // default, no rules
    private StringBuilder loadedRules = new StringBuilder();
    private Date lastLoadDate = new Date();

    private volatile boolean needRefresh = true;

    private int logWarningCounter = 0;
    private int logWarningLimit = 10;

    public boolean needReloading() {
        return needRefresh;
    }

    /**
     * Load rules for given path. If no path is provided, default will be used (configured in spring configuration)
     *
     * @param context servlet context
     * @param request servlet request
     * @param urlRewriteLocation absolute repository path
     */
    @Deprecated
    public synchronized StringBuilder loadRules(final ServletContext context, final ServletRequest request, final String urlRewriteLocation) {
        return load(context, request, null);
    }


    /**
    * Load rules for given path. If no path is provided, default will be used (configured in spring configuration)
    *
    * @param context servlet context
    * @param request servlet request
    * @param rewriteRulesLocation absolute repository path of the rules
    */
    public synchronized StringBuilder load(final ServletContext context, final ServletRequest request, final String rewriteRulesLocation) {
        // check if refresh is needed..if not return local copy
        if (!needRefresh) {
            return loadedRules;
        }

        String rulesLocation = getRulesLocation(rewriteRulesLocation);
        if(StringUtils.isBlank(rulesLocation)){
            log.error("No location specified for rules. Cannot load rules.");
            return null;
        }

        log.debug("Loading rules from location {}", rulesLocation);

        Session session = null;
        StringBuilder rules = new StringBuilder(UrlRewriteConstants.XML_PROLOG);
        rules.append(UrlRewriteConstants.XML_START);
        try {
            session = getSession();
            if (session == null) {
                return null;
            }

            if (!session.getRootNode().hasNode(rulesLocation.substring(1))) {
                // HIPPLUG-963 - after x amount of warning, don't display any log anymore
                // because otherwise warning will fill up the logs.
                if (logWarningCounter <= logWarningLimit) {
                    log.warn("{} Location {} is not yet available, should retry", logWarningCounter, rulesLocation);
                    logWarningCounter++;
                    return null;
                }
                return null;
            }

            Node rootNode = session.getRootNode().getNode(rulesLocation.substring(1));
            if(! rootNode.hasNodes()){
                log.debug("No rules found under {}.", UrlRewriteUtils.getJcrItemPath(rootNode));
                return null;
            }

            ignoreContextPath = rootNode.hasProperty(UrlRewriteConstants.IGNORE_CONTEXT_PATH_PROPERTY) ?
                    Boolean.valueOf(rootNode.getProperty(UrlRewriteConstants.IGNORE_CONTEXT_PATH_PROPERTY).getString()) :
                    UrlRewriteConstants.IGNORE_CONTEXT_PATH_PROPERTY_DEFAULT_VALUE;

            boolean useQueryString = rootNode.hasProperty(UrlRewriteConstants.USE_QUERY_STRING_PROPERTY) ?
                    Boolean.valueOf(rootNode.getProperty(UrlRewriteConstants.USE_QUERY_STRING_PROPERTY).getString()) :
                    UrlRewriteConstants.USE_QUERY_STRING_PROPERTY_DEFAULT_VALUE;

            skipPOST = rootNode.hasProperty(UrlRewriteConstants.SKIP_POST_PROPERTY) ?
                    Boolean.valueOf(rootNode.getProperty(UrlRewriteConstants.SKIP_POST_PROPERTY).getString()) :
                    UrlRewriteConstants.SKIP_POST_PROPERTY_DEFAULT_VALUE;

            if(rootNode.hasProperty(UrlRewriteConstants.SKIPPED_PREFIXES_PROPERTY)){
                Value[] prefixValues = rootNode.getProperty(UrlRewriteConstants.SKIPPED_PREFIXES_PROPERTY).getValues();
                skippedPrefixes = new String[prefixValues.length];
                for(int i=0; i < prefixValues.length; i++) {
                    skippedPrefixes[i] = prefixValues[i].getString();
                }
            }

            if(!ignoreContextPath) {
              rules.append(" use-context=\"true\"");
            }

            if(useQueryString) {
              rules.append(" use-query-string=\"true\"");
            }

            // HIPPLUG-476: always disable decoding as it can interfere with hst encodings
            rules.append(" decode-using=\"null\"");

            rules.append(">");

            // Start recursion
            load(rootNode, context, rules);

        } catch (Exception e) {
            log.error("Error loading rewriting rules in {}", rulesLocation, e);
        } finally {
            closeSession(session);
        }

        rules.append(UrlRewriteConstants.XML_END);

        // Update our state
        loadedRules = new StringBuilder(rules);
        needRefresh = false;
        lastLoadDate = new Date();

        return rules;
    }

    /**
     * Load rules recursively starting from a urlrewriter:ruleset node
     *
     * @param startNode configured starting location for rules
     * @param context the servlet context
     * @param rules StringBuilder to load the rules in
     */
    private void load(final Node startNode, final ServletContext context, final StringBuilder rules) throws RepositoryException {

        final List<String> version101Documents = new ArrayList<String>();

        final NodeIterator nodes = startNode.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();

            // version 1.02.xx ruleset is a folder, version 1.01.xx used standard folders
            if (node.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_RULESET)
                    || node.isNodeType(HippoStdNodeType.NT_FOLDER)
                    || node.isNodeType(HippoStdNodeType.NT_DIRECTORY)){
                load(node, context, rules);
            } else {
                node = getDocumentNode(node);
                if(node == null){
                    continue;
                }

                // ..but in 1.01.xx version, the ruleset was a document
                if (node.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_101xx_RULESET_DOCUMENT)
                    || node.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_101xx_RULESETXML_DOCUMENT)) {
                    version101Documents.add(node.getPath());
                }
                else {

                    String rule;
                    for (RewritingRulesExtractor rulesExtractor : rewritingRulesExtractors){
                        try{
                            rule = rulesExtractor.extract(node, context);
                            if(rule != null){
                                rules.append(rule);
                            }
                        } catch (RepositoryException e){
                            log.error("Exception encountered while extracting with: " + rulesExtractor, e);
                        }
                    }
                }
            }
        }

        // construct an informative message and log as error to always show up
        if (!version101Documents.isEmpty()) {

            final StringBuilder builder = new StringBuilder();
            builder.append("IMPORTANT: ");
            if (version101Documents.size() == 1) {
                builder.append("There is 1 rewriter rule of version 1.01.xx present in the repository. It is skipped and will NOT be processed. Locations: ");
                builder.append(version101Documents.get(0));
            }
            else {
                builder.append("There are ");
                builder.append(version101Documents.size());
                builder.append(" rewriter rules of version 1.01.xx present in the repository. These are skipped and will NOT be processed. ");
                builder.append("First 2 found rules are at locations: \n");
                builder.append(version101Documents.get(0)).append(" and \n");
                builder.append(version101Documents.get(1)).append(". \n");
            }
            builder.append("Please create new rules based on old rules of 1.01.xx style.");

            log.error(builder.toString());
        }
    }


    protected Node getDocumentNode(Node wrapperNode) throws RepositoryException{
        if (wrapperNode.isNodeType(HippoNodeType.NT_HANDLE)) {
            NodeIterator docs = wrapperNode.getNodes(wrapperNode.getName());
            Node document;
            while (docs.hasNext()) {
                document = docs.nextNode();
                if (document.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
                    Property availabities = document.getProperty(HippoNodeType.HIPPO_AVAILABILITY);
                    for (Value availabity : availabities.getValues()) {
                        if (availabity.getString().equals("live")) {
                            return document;
                        }
                    }
                }
            }
        }
        return null;
    }


    protected String getRulesLocation(String overrideRulesLocation){
        String rulesLocation = overrideRulesLocation;
        if(StringUtils.isBlank(rulesLocation)){
            log.debug("Filter configuration does not specify UrlRewriteLocation, or is not an absolute path. Will use default one: {}", getRewriteRulesLocation());
            rulesLocation = getRewriteRulesLocation();
            if (rulesLocation == null || !rulesLocation.startsWith("/")) {
                return null;
            }
        }
        return rulesLocation;
    }

    public boolean getIgnoreContextPath(){
          return ignoreContextPath;
    }

    public boolean getSkipPOST(){
          return skipPOST;
    }

    public String[] getSkippedPrefixes(){
          return skippedPrefixes;
    }

    public void invalidate(final Event event) {
        needRefresh = true;
    }

    public Date getLastLoadDate() {
        return lastLoadDate;
    }

    protected Session getSession() {
        Session session = null;
        try {
            session = repository.login(credentials);
        } catch (RepositoryException e) {
            log.error("Error obtaining session", e);
        }
        return session;
    }

    protected void closeSession(final Session session) {
        if (session != null) {
            session.logout();
        }
    }



    //*************************************************************************************
    // SPRING MANAGED PROPERTIES
    //*************************************************************************************


    public String getRewriteRulesLocation() {
        return rewriteRulesLocation;
    }

    public void setRewriteRulesLocation(final String rewriteRulesLocation) {
        this.rewriteRulesLocation = rewriteRulesLocation;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(final Repository repository) {
        this.repository = repository;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(final Credentials credentials) {
        this.credentials = credentials;
    }

    public List<RewritingRulesExtractor> getRewritingRulesExtractors() {
        return rewritingRulesExtractors;
    }

    public void setRewritingRulesExtractors(final List<RewritingRulesExtractor> rewritingRulesExtractors) {
        this.rewritingRulesExtractors = rewritingRulesExtractors;
    }



}
