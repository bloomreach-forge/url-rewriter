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
package org.onehippo.forge.rewriting.repo;

import java.util.Date;

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.observation.Event;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.tools.ant.filters.StringInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * @version $Id$
 */
public class RewritingManager {


    private static Logger log = LoggerFactory.getLogger(RewritingManager.class);
    private static final String XML_START = "<?xml version=\"1.0\" encoding=\"utf-8\"?><urlrewrite>";
    private static final String XML_END = "</urlrewrite>";
    private static final String QUERY_LIMIT = "//**element(*, urlrewriter:basedocument) [@hippostd:state ='published']";
    private static final String PRIMARY_TYPE_XML = "urlrewriter:rule";
    private static final String XML_TYPE = "urlrewriter:rulesetxml";
    private static final String FROM_PROPERTY = "urlrewriter:rulefrom";
    private static final String TO_PROPERTY = "urlrewriter:ruleto";
    private static final String TYPE_PROPERTY = "urlrewriter:ruletype";
    private static final String CASE_SENSITIVE_PROPERTY = "urlrewriter:casesensitive";
    private static final String NAME_PROPERTY = "urlrewriter:rulename";
    private static final String AND_OR_PROPERTY = "urlrewriter:conditionor";

    private volatile boolean needRefresh = true;

    // spring managed
    private String urlRewritingLocation;

    private Repository repository;

    private Credentials credentials;

    // default, no rules
    private StringBuilder loadedRules = new StringBuilder();
    private Date lastLoadDate = new Date();

    public boolean needReloading() {
        return needRefresh;
    }


    /**
     * Load rules for given path. If path is no provided, default will be used (configured in spring configuration)
     *
     * @param context
     * @param request
     * @param urlRewriteLocation absolute repository path
     */
    public synchronized StringBuilder loadRules(final ServletContext context, final ServletRequest request, final String urlRewriteLocation) {
        // check if refresh is needed..if not return local copy
        if (!needRefresh) {

            return loadedRules;
        }
        StringBuilder rules = new StringBuilder(XML_START);
        log.debug("Loading rules for: urlRewriteLocation {}", urlRewriteLocation);
        String path = urlRewriteLocation;
        if (urlRewriteLocation == null || !urlRewriteLocation.startsWith("/")) {
            log.debug("urlRewriteLocation was null, or wasn't absolute path, will use default one: {}", getUrlRewritingLocation());
            path = getUrlRewritingLocation();
        }
        Session session = null;
        try {
            session = getNewSession();
            if (session == null) {
                return null;
            }
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery('/' + path + QUERY_LIMIT, "xpath");
            QueryResult result = query.execute();
            NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                rules.append(extractRules(node, context));
            }
            needRefresh = false;
            lastLoadDate = new Date();
        } catch (Exception e) {
            log.error("Error loading rewriting rules {}", e);
        } finally {
            closeSession(session);
        }

        rules.append(XML_END);
        loadedRules = new StringBuilder(rules);
        return rules;
    }


    private String extractRules(final Node node, final ServletContext request) {
        StringBuilder builder = new StringBuilder();
        try {
            // check if xml type:
            String primaryType = node.getPrimaryNodeType().getName();
            if (primaryType.equals(XML_TYPE)) {
                Value[] values = node.getProperty(PRIMARY_TYPE_XML).getValues();
                for (Value value : values) {
                    String rule = value.getString();
                    if (validRule(rule, request)) {
                        builder.append(rule);
                    }
                }

            } else {
                // these are simple, compound types:
                NodeIterator nodes = node.getNodes(PRIMARY_TYPE_XML);
                while (nodes.hasNext()) {
                    Node ruleNode = nodes.nextNode();
                    String rule = extractCompoundRule(ruleNode);
                    if (rule != null && validRule(rule, request)) {
                        builder.append(rule);
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("error getting rules {}", e);
        }
        return builder.toString();
    }

    private String extractCompoundRule(final Node ruleNode) {

        String from = extractProperty(ruleNode, FROM_PROPERTY);
        if (from == null) {
            return null;
        }
        String to = extractProperty(ruleNode, TO_PROPERTY);
        if (to == null) {
            return null;
        }


        String type = extractProperty(ruleNode, TYPE_PROPERTY);
        boolean caseSensitive = extractBooleanProperty(ruleNode, CASE_SENSITIVE_PROPERTY);

        String ruleFrom;
        if (caseSensitive) {
            ruleFrom = new StringBuilder().append("<from casesensitive=\"true\">").append(from).append("</from>").toString();
        } else {
            ruleFrom = new StringBuilder().append("<from>").append(from).append("</from>").toString();
        }

        String ruleTo;
        if (type != null) {
            ruleTo = new StringBuilder().append("<to type=\"").append(type).append("\">").append(to).append("</to>").toString();
        } else {
            ruleTo = new StringBuilder().append("<to>").append(to).append("</to>").toString();
        }
        StringBuilder builder = new StringBuilder();
        builder.append("<rule>");
        String ruleName = extractProperty(ruleNode, NAME_PROPERTY);
        if (!StringUtils.isBlank(ruleName)) {
            builder.append("<name>").append(ruleName).append("</name>");
        }
        String conditions = parseConditionals(ruleNode);
        if (conditions != null) {
            builder.append(conditions);
        }
        return builder.append(ruleFrom).append(ruleTo).append("</rule>").toString();
    }


    private String parseConditionals(final Node ruleNode) {
        try {
            NodeIterator conditions = ruleNode.getNodes("urlrewriter:rulecondition");
            StringBuilder builder = new StringBuilder();
            while (conditions.hasNext()) {
                Node conditionNode = conditions.nextNode();
                String condition = extractCondition(conditionNode);
                if (condition != null) {
                    builder.append(condition);
                }
            }
            if (builder.length() > 0) {
                return builder.toString();
            }

        } catch (RepositoryException e) {
            log.error("Error parsing rules", e);
        }
        return null;
    }

    private String extractCondition(final Node conditionNode) {
        String name = extractProperty(conditionNode, "urlrewriter:conditionpredefinedname");
        if (name == null) {
            name = extractProperty(conditionNode, "urlrewriter:conditionname");
        }
        String value = extractProperty(conditionNode, "urlrewriter:conditionvalue");
        if (value == null) {
            log.warn("Invalid URL rewrite condition '{}' on node '{}': value was null", name,
                    getJcrItemPath(conditionNode));
            return null;
        }
        String type = extractProperty(conditionNode, "urlrewriter:conditiontype");
        String operator = extractProperty(conditionNode, "urlrewriter:conditionoperator");
        if (name == null && operator == null && type == null) {
            log.warn("Invalid URL rewrite condition on node '{}': all parameters [name, type, operator] are null",
                    getJcrItemPath(conditionNode));
            return null;
        }
        String booleanCondition = extractProperty(conditionNode, AND_OR_PROPERTY);
        StringBuilder builder = new StringBuilder();
        builder.append("<condition ");
        if (!StringUtils.isBlank(type)) {
            builder.append("type=\"").append(type).append("\" ");
        }
        if (!StringUtils.isBlank(name)) {
            builder.append("name=\"").append(name).append("\" ");
        }

        if (!StringUtils.isBlank(booleanCondition) && booleanCondition.equals("or")) {
            builder.append("next=\"").append(booleanCondition).append("\" ");
        }
        if (!StringUtils.isBlank(operator)) {
            builder.append("operator=\"").append(operator).append("\" ");
        }
        builder.append('>').append(value).append("</condition>");

        return builder.toString();
    }


    private Boolean extractBooleanProperty(final Node node, final String property) {
        try {
            if (node.hasProperty(property)) {
                Property p = node.getProperty(property);
                return p.getBoolean();
            }
        } catch (Exception ignore) {
            // ignore
        }
        return false;
    }

    private String extractProperty(final Node node, final String property) {
        try {
            if (node.hasProperty(property)) {
                Property p = node.getProperty(property);
                return StringUtils.trimToNull(p.getString());
            }
        } catch (RepositoryException ignore) {
            // ignore
        }
        return null;

    }

    /**
     * Validates the rule
     *
     * @param rule    xml string containing rule
     * @param context
     * @return
     */
    private boolean validRule(final String rule, final ServletContext context) {

        Conf conf = new Conf(context, new StringInputStream(XML_START + rule + XML_END), "testing-valid-",
                "testing-valid-rules", false);
        boolean ok = conf.isOk();
        if (!ok) {
            log.warn("skipping invalid rule:  {}", rule);
        }
        return ok;
    }


    private Session getNewSession() {
        Session session = null;
        try {
            session = repository.login(credentials);
        } catch (RepositoryException e) {
            log.error("Error obtaining session {}", e);
        }
        return session;
    }

    private void closeSession(final Session session) {
        if (session != null) {
            session.logout();
        }
    }


    public void invalidate(final Event event) {
        needRefresh = true;
    }


    //*************************************************************************************
    // SPRING MANAGED PROPERTIES
    //*************************************************************************************
    public String getUrlRewritingLocation() {
        return urlRewritingLocation;
    }

    public void setUrlRewritingLocation(final String urlRewritingLocation) {
        this.urlRewritingLocation = urlRewritingLocation;
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

    public Date getLastLoadDate() {
        return lastLoadDate;
    }

    /**
     * JCR item path getter
     * @param item JCR item
     * @return Path (nullable)
     */
    private static String getJcrItemPath(Item item) {
        if (item == null) {
            return null;
        }
        try {
            return item.getPath();
        } catch (RepositoryException e) {
            log.warn(e.getMessage());
            return null;
        }
    }
}
