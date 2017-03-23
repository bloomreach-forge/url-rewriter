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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletContext;

import org.apache.tools.ant.filters.StringInputStream;
import org.hippoecm.hst.util.XmlUtils;
import org.onehippo.forge.rewriting.UrlRewriteConstants;
import org.onehippo.forge.rewriting.UrlRewriteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * @version $Id$
 */
public abstract class AbstractRulesExtractor implements RewritingRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(AbstractRulesExtractor.class);


    public abstract String extract(final Node node, final ServletContext context) throws RepositoryException;


    //*************************************************************************************
    // Common utility methods for any RewritingRulesExtractor
    //*************************************************************************************

    protected Boolean extractBooleanProperty(final Node node, final String property) {
        try {
            if (node.hasProperty(property)) {
                Property p = node.getProperty(property);
                return p.getBoolean();
            }
        } catch (Exception e) {
            log.warn("Exception while accessing property {}, on node {}", property, UrlRewriteUtils.getJcrItemPath(node));
        }
        return false;
    }

    protected String extractProperty(final Node node, final String property) {
        return extractProperty(node, property, true);
    }

    protected String extractProperty(final Node node, final String property, final boolean xmlEscaping) {
        try {
            if (node.hasProperty(property)) {
                Property p = node.getProperty(property);
                String value = StringUtils.trimToNull(p.getString());
                return value != null && xmlEscaping ?
                        XmlUtils.encode(value) :
                        value;
            }
        } catch (RepositoryException e) {
            log.warn("Exception while accessing property {}, on node {}", property, UrlRewriteUtils.getJcrItemPath(node));
        }
        return null;
    }

    protected Value[] extractMultipleProperty(final Node node, final String property) {
        try {
            if (node.hasProperty(property)) {
                return node.getProperty(property).getValues();
            }
        } catch (RepositoryException ignore) {
            return new Value[0];
        }
        return new Value[0];
    }

    /**
     * Validates a rule
     *
     * @param rule    xml string containing rule
     * @param context
     * @return
     */
    protected boolean validateRule(final String rule, final ServletContext context) {
        if (StringUtils.isBlank(rule)) {
            return false;
        }
        Conf conf = new Conf(context,
                 new StringInputStream(UrlRewriteConstants.XML_PROLOG + UrlRewriteConstants.XML_START + ">" + rule + UrlRewriteConstants.XML_END, "UTF-8"),
                "testing-valid-",
                "testing-valid-rules",
                false);
        boolean ok = conf.isOk();
        if (!ok) {
            log.warn("skipping invalid rule:  {}", rule);
        }
        return ok;
    }

    /**
     * Creates conditions for the scheme and domain
     *
     * @param scheme String holding the scheme
     * @param domain String holding the domain
     * @param port Integer holding the port
     * @return An xml representation of the rule condition
     */
    protected String createSchemeAndDomainCondition(final String scheme, final String domain, final int port) {
        return new StringBuilder()
                .append("<condition type=\"scheme\" operator=\"equal\" next=\"and\">")
                .append(scheme)
                .append("</condition>")
                .append("<condition type=\"header\" name=\"host\" operator=\"equal\" next=\"and\">")
                .append(domain)
                .append((port != -1) ? (":" + port) : "")
                .append("</condition>")
                .toString();
    }

    public String toString() {
        return getClass().getName();
    }

}
