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

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class XmlRulesExtractor extends AbstractRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(XmlRulesExtractor.class);



    @Override
    public String load(final ServletContext context, final ServletRequest request) {

        String rulesLocation = this.getRewriteRulesLocation();
       /* if (rulesLocation == null || !rulesLocation.startsWith("/")) {
            log.error("No location specified for complex rules. Cannot extract rules.");
            return null;
        }
        log.debug("Loading complex rules from location {}", rulesLocation);

        Session session = null;
        StringBuilder rules = new StringBuilder();
        try {
            session = getSession();
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
            log.error("Error loading complex rewriting rules {}", e);
        } finally {
            closeSession(session);
        }
        return rules.toString();*/
        return null;
    }

/*


    private String extractRules(final Node node, final ServletContext context) {
        StringBuilder builder = new StringBuilder();
        try {
            // check if xml type:
            String primaryType = node.getPrimaryNodeType().getName();
            if (primaryType.equals(UrlRewriteConstants.PRIMARY_TYPE_RULESETXML)) {
                Value[] values = extractMultipleProperty(node, UrlRewriteConstants.PRIMARY_TYPE_RULE);
                for (Value value : values) {
                    String rule = value.getString();
                    if (validateRule(rule, context)) {
                        builder.append(rule);
                    }
                }

            } else {
                // these are simple, compound types:
                NodeIterator nodes = node.getNodes(UrlRewriteConstants.PRIMARY_TYPE_RULE);
                while (nodes.hasNext()) {
                    Node ruleNode = nodes.nextNode();
                    String rule = extractCompoundRule(ruleNode);
                    if (validateRule(rule, context)) {
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
        String name = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_PREDEFINED_NAME_PROPERTY);
        if (name == null) {
            name = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_NAME_PROPERTY);
        }
        String value = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_VALUE_PROPERTY);
        if (value == null) {
            log.warn("Invalid URL rewrite condition '{}' on node '{}': value was null", name,
                    getJcrItemPath(conditionNode));
            return null;
        }
        String type = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_TYPE_PROPERTY);
        String operator = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_OPERATOR_PROPERTY);
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

*/


}
