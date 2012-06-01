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
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;

import org.onehippo.forge.rewriting.UrlRewriteConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class XmlRulesExtractor extends AbstractRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(XmlRulesExtractor.class);

    @Override
    public String extract(final Node ruleNode, final ServletContext context, final boolean ignoreContextPath) throws RepositoryException {

        if (!ruleNode.isNodeType(UrlRewriteConstants.PRIMARY_TYPE_XMLRULE)) {
            return null;
        }
        String rule = extractProperty(ruleNode, UrlRewriteConstants.XML_RULE_PROPERTY, false);
        return validateRule(rule, context) ? rule : null;
    }

}
