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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.forge.rewriting.UrlRewriteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * @version $Id$
 */
public class SimpleRulesExtractor extends AbstractRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(SimpleRulesExtractor.class);

    @Override
    public String load(final ServletContext context, final ServletRequest request) {
        String rulesLocation = this.getRewriteRulesLocation();
        if (rulesLocation == null || !rulesLocation.startsWith("/")) {
            log.error("No location specified for simple rules. Cannot extract rules.");
            return null;
        }
        log.debug("Loading simple rules from location {}", rulesLocation);

        Session session = null;
        StringBuilder rules = new StringBuilder();
        try {
            session = getSession();
            if (session == null) {
                return null;
            }

            Node rootNode = session.getRootNode().getNode(rulesLocation.substring(1));
            if(! rootNode.hasNodes()){
                log.debug("No simple rules found under {}.", getJcrItemPath(rootNode));
                return null;
            }

            load(rootNode, context, rules);

        } catch (Exception e) {
            log.error("Error loading simple rewriting rules {}", e);
        } finally {
            closeSession(session);
        }
        return rules.toString();

    }

    private void load(final Node startNode, final ServletContext context, final StringBuilder rules) throws RepositoryException {
        NodeIterator nodes = startNode.getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            if(node.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_SIMPLEFOLDER)){
                load(node, context, rules);
            } else {
                String rule = extractRule(node, context);
                if (validateRule(rule, context)) {
                    rules.append(rule);
                }
            }
        }
    }

    private String extractRule(final Node node, final ServletContext context) throws RepositoryException {

        //passed node can only be a handle
        if (! node.isNodeType(HippoNodeType.NT_HANDLE)) {
            return null;
        }

        //TODO Is this ok???????
        Node simpleRuleNode = node.getNode(node.getName());
        if(! simpleRuleNode.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_SIMPLERULE)){
            return null;
        }

        String ruleName = simpleRuleNode.getName();
        String ruleDescription = extractProperty(simpleRuleNode, UrlRewriteConstants.DESCRIPTION_PROPERTY);

        String ruleFrom = extractProperty(simpleRuleNode, UrlRewriteConstants.FROM_PROPERTY);
        String ruleTo = extractProperty(simpleRuleNode, UrlRewriteConstants.TO_PROPERTY);
        if (ruleFrom == null || ruleTo == null) {
            return null;
        }

        String type = extractProperty(simpleRuleNode, UrlRewriteConstants.TYPE_PROPERTY);
        boolean caseSensitive = extractBooleanProperty(simpleRuleNode, UrlRewriteConstants.CASE_SENSITIVE_PROPERTY);

        ruleFrom = caseSensitive ?
                new StringBuilder().append("<from casesensitive=\"true\">").append(ruleFrom).append("</from>").toString() :
                new StringBuilder().append("<from>").append(ruleFrom).append("</from>").toString();

        ruleTo = type != null ?
                new StringBuilder().append("<to type=\"").append(type).append("\">").append(ruleTo).append("</to>").toString():
                new StringBuilder().append("<to>").append(ruleTo).append("</to>").toString();


        StringBuilder builder = new StringBuilder();
        builder.append("<rule>")
                .append("<name>")
                .append(StringUtils.isBlank(ruleName) ? "" : ruleName)
                .append(StringUtils.isBlank(ruleDescription) ? "" : ((StringUtils.isBlank(ruleName) ? "" : " - ") + ruleDescription))
                .append("</name>")
                .append(ruleFrom)
                .append(ruleTo)
                .append("</rule>");

        return builder.toString();
    }

}
