/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.servlet.ServletContext;

import org.hippoecm.hst.util.XmlUtils;
import org.onehippo.forge.rewriting.UrlRewriteConstants;
import org.onehippo.forge.rewriting.UrlRewriteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * @version $Id$
 */
public class AdvancedRulesExtractor extends AbstractRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(AdvancedRulesExtractor.class);

    @Override
    public String extract(final Node ruleNode, final ServletContext context) throws RepositoryException {

        if(! ruleNode.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_ADVANCEDRULE)){
            return null;
        }

        String ruleName = XmlUtils.encode(ruleNode.getName());
        String ruleDescription = extractProperty(ruleNode, UrlRewriteConstants.DESCRIPTION_PROPERTY);

        String ruleFrom = extractProperty(ruleNode, UrlRewriteConstants.FROM_PROPERTY);
        String ruleTo = extractProperty(ruleNode, UrlRewriteConstants.TO_PROPERTY);
        if (ruleFrom == null || ruleTo == null) {
            return null;
        }

        String type = extractProperty(ruleNode, UrlRewriteConstants.TYPE_PROPERTY);
        boolean caseSensitive = extractBooleanProperty(ruleNode, UrlRewriteConstants.CASE_SENSITIVE_PROPERTY);
        boolean isWildCardType= extractBooleanProperty(ruleNode, UrlRewriteConstants.IS_WILDCARD_TYPE_PROPERTY);
        boolean isNotLast = extractBooleanProperty(ruleNode, UrlRewriteConstants.IS_NOT_LAST_PROPERTY);
        boolean qsAppend = extractBooleanProperty(ruleNode, UrlRewriteConstants.QSAPPEND_PROPERTY);

        ruleFrom = new StringBuilder("<from")
                .append(caseSensitive ? " casesensitive=\"true\"" : "")
                .append(">")
                .append(ruleFrom).append("</from>").toString();

        ruleTo = new StringBuilder("<to type=\"")
                .append(type != null ? type : UrlRewriteConstants.DEFAULT_RULE_TYPE).append("\"")
                .append(isNotLast ? "" : " last=\"true\"")
                .append(qsAppend ? " qsappend=\"true\"" : "")
                .append(">")
                .append(ruleTo).append("</to>").toString();

        StringBuilder builder = new StringBuilder();
        builder.append("<rule")
                .append(isWildCardType ? " match-type=\"wildcard\">" : ">")
                .append("<name>")
                .append(StringUtils.isBlank(ruleName) ? "" : ruleName)
                .append(StringUtils.isBlank(ruleDescription) ? "" : ((StringUtils.isBlank(ruleName) ? "" : " - ") + ruleDescription))
                .append("</name>");

        String conditions = parseConditionals(ruleNode);
        if (conditions != null) {
            builder.append(conditions);
        }

        builder.append(ruleFrom).append(ruleTo).append("</rule>");

        String rule = builder.toString();
        return validateRule(rule, context) ? rule : null;
    }

    private String parseConditionals(final Node ruleNode) {
        try {
            NodeIterator conditions = ruleNode.getNodes(UrlRewriteConstants.CONDITIONS_NODE);
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
        String name = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_NAME_PROPERTY);

        String value = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_VALUE_PROPERTY);
        if (value == null) {
            log.warn("Invalid URL rewrite condition '{}' on node '{}': value was null", name,
                    UrlRewriteUtils.getJcrItemPath(conditionNode));
            return null;
        }
        String type = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_TYPE_PROPERTY);
        String operator = extractProperty(conditionNode, UrlRewriteConstants.CONDITION_OPERATOR_PROPERTY);
        if (operator == null && type == null) {
            log.warn("Invalid URL rewrite condition on node '{}': all parameters [name, type, operator] are null", UrlRewriteUtils.getJcrItemPath(conditionNode));
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



}
