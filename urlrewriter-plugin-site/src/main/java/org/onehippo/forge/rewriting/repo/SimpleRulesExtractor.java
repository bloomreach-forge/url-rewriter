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

import java.net.URL;

import javax.jcr.Node;
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
public class SimpleRulesExtractor extends AbstractRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(SimpleRulesExtractor.class);

    @Override
    public String extract(final Node ruleNode, final ServletContext context, final boolean ignoreContextPath) throws RepositoryException {

        if(! ruleNode.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_SIMPLERULE)){
            return null;
        }

        String ruleName = XmlUtils.encode(ruleNode.getName());
        String ruleDescription = extractProperty(ruleNode, UrlRewriteConstants.DESCRIPTION_PROPERTY);

        String ruleFrom = extractProperty(ruleNode, UrlRewriteConstants.FROM_PROPERTY);
        String ruleTo = extractProperty(ruleNode, UrlRewriteConstants.TO_PROPERTY);
        if (ruleFrom == null || ruleTo == null) {
            return null;
        }

        URL urlFrom = UrlRewriteUtils.parseUrl(ruleFrom);
        if(urlFrom == null){
            return null;
        }
        ruleFrom = urlFrom.getFile() + (!StringUtils.isBlank(urlFrom.getRef()) ? "#" + urlFrom.getRef() : "");

        String type = extractProperty(ruleNode, UrlRewriteConstants.TYPE_PROPERTY);
        boolean caseSensitive = extractBooleanProperty(ruleNode, UrlRewriteConstants.CASE_SENSITIVE_PROPERTY);

        ruleFrom = caseSensitive ?
                new StringBuilder().append("<from casesensitive=\"true\">").append(ruleFrom).append("</from>").toString() :
                new StringBuilder().append("<from>").append(ruleFrom).append("</from>").toString();

        ruleTo = type != null ?
                new StringBuilder().append("<to type=\"").append(type).append("\">").append(ruleTo).append("</to>").toString():
                new StringBuilder().append("<to type=\"").append(UrlRewriteConstants.DEFAULT_RULE_TYPE).append("\">").append(ruleTo).append("</to>").toString();


        StringBuilder builder = new StringBuilder();
        builder.append("<rule match-type=\"wildcard\">")
                .append("<name>")
                .append(StringUtils.isBlank(ruleName) ? "" : ruleName)
                .append(StringUtils.isBlank(ruleDescription) ? "" : ((StringUtils.isBlank(ruleName) ? "" : " - ") + ruleDescription))
                .append("</name>");

        String domain = urlFrom.getHost();
        if(domain != null){
            builder.append(createDomainCondition(domain));
        }

        builder.append(ruleFrom)
                .append(ruleTo)
                .append("</rule>");

        String rule = builder.toString();
        return validateRule(rule, context) ? rule : null;
    }

}
