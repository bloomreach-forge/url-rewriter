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

import javax.jcr.Credentials;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
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
public abstract class AbstractRulesExtractor implements RewritingRulesExtractor {

    private static Logger log = LoggerFactory.getLogger(AbstractRulesExtractor.class);


    public abstract String load(final ServletContext context, final ServletRequest request);


    //*************************************************************************************
    // Common utility methods for any RewritingRulesExtractor
    //*************************************************************************************
    protected Session getSession() {
        Session session = null;
        try {
            session = repository.login(credentials);
        } catch (RepositoryException e) {
            log.error("Error obtaining session {}", e);
        }
        return session;
    }

    protected void closeSession(final Session session) {
        if (session != null) {
            session.logout();
        }
    }

    protected Boolean extractBooleanProperty(final Node node, final String property) {
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

    protected String extractProperty(final Node node, final String property) {
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

    protected Value[] extractMultipleProperty(final Node node, final String property){
        try{
            if(node.hasProperty(property)){
                return  node.getProperty(property).getValues();
            }
        } catch (RepositoryException ignore) {
            return new Value[0];
        }
        return new Value[0];
    }

    /**
     * JCR item path getter
     *
     * @param item JCR item
     * @return Path (nullable)
     */
    protected String getJcrItemPath(Item item) {
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

    /**
     * Validates a rule
     *
     * @param rule    xml string containing rule
     * @param context
     * @return
     */
    protected boolean validateRule(final String rule, final ServletContext context) {
        if(StringUtils.isBlank(rule)){
            return false;
        }
        Conf conf = new Conf(context, new StringInputStream(UrlRewriteConstants.XML_START + rule + UrlRewriteConstants.XML_END), "testing-valid-", "testing-valid-rules", false);
        boolean ok = conf.isOk();
        if (!ok) {
            log.warn("skipping invalid rule:  {}", rule);
        }
        return ok;
    }


    //*************************************************************************************
    // SPRING MANAGED PROPERTIES
    //*************************************************************************************
    protected String rewriteRulesLocation;
    protected Repository repository;
    protected Credentials credentials;

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
}
