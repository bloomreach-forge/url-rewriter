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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.tools.ant.filters.StringInputStream;
import org.onehippo.forge.rewriting.UrlRewriteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * @version $Id$
 */
public class ComplexRulesExtractor extends AbstractRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(ComplexRulesExtractor.class);

    private static final String QUERY_LIMIT = "//*[@hippostd:state ='published' and (@jcr:primaryType='urlrewriter:ruleset' or @jcr:primaryType='urlrewriter:rulesetxml')]";

    @Override
    public String load(final ServletContext context, final ServletRequest request) {

        String rulesLocation = this.getRewriteRulesLocation();
        if (rulesLocation == null || !rulesLocation.startsWith("/")) {
            log.error("No location specified for complex rules. Cannot extract rules.");
            return null;
        }
        log.debug("Loading complex rules from location {}", rulesLocation);

        Session session = null;
        StringBuilder rules = new StringBuilder();
        try {
            session = getNewSession();
            if (session == null) {
                return null;
            }
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            Query query = queryManager.createQuery('/' + rulesLocation + QUERY_LIMIT, "xpath");
            QueryResult result = query.execute();
            NodeIterator nodes = result.getNodes();
            while (nodes.hasNext()) {
                Node node = nodes.nextNode();
                rules.append(extractRules(node, context));
            }
        } catch (Exception e) {
            log.error("Error loading rewriting rules {}", e);
        } finally {
            closeSession(session);
        }
        return rules.toString();
    }



    private String extractRules(final Node node, final ServletContext request) {
        StringBuilder builder = new StringBuilder();
        try {
            // check if xml type:
            String primaryType = node.getPrimaryNodeType().getName();
            if (primaryType.equals(UrlRewriteConstants.XML_TYPE)) {
                Value[] values = node.getProperty(UrlRewriteConstants.PRIMARY_TYPE_XML).getValues();
                for (Value value : values) {
                    String rule = value.getString();
                    if (validRule(rule, request)) {
                        builder.append(rule);
                    }
                }

            } else {
                // these are simple, compound types:
                NodeIterator nodes = node.getNodes(UrlRewriteConstants.PRIMARY_TYPE_XML);
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

        String from = extractProperty(ruleNode, UrlRewriteConstants.FROM_PROPERTY);
        if (from == null) {
            return null;
        }
        String to = extractProperty(ruleNode, UrlRewriteConstants.TO_PROPERTY);
        if (to == null) {
            return null;
        }


        String type = extractProperty(ruleNode, UrlRewriteConstants.TYPE_PROPERTY);
        boolean caseSensitive = extractBooleanProperty(ruleNode, UrlRewriteConstants.CASE_SENSITIVE_PROPERTY);

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
        String ruleName = extractProperty(ruleNode, UrlRewriteConstants.NAME_PROPERTY);
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
        String booleanCondition = extractProperty(conditionNode, UrlRewriteConstants.AND_OR_PROPERTY);
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

        Conf conf = new Conf(context, new StringInputStream(UrlRewriteConstants.XML_START + rule + UrlRewriteConstants.XML_END), "testing-valid-",
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
