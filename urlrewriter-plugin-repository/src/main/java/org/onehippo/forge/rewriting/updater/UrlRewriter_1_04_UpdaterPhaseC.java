/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.rewriting.updater;

import java.io.BufferedInputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This phase loads the new cnd and upgraded namespaces
 * @version $Id$
 */
public class UrlRewriter_1_04_UpdaterPhaseC implements UpdaterModule {


    protected static final Logger log = LoggerFactory.getLogger(UrlRewriter_1_04_UpdaterPhaseC.class);

    public void register(final UpdaterContext context) {

        context.registerName("url-rewriter-updater-1-04-phaseC");
        context.registerStartTag("url-rewriter-updater-1-04-phaseB");
        context.registerEndTag("url-rewriter-updater-1-04-phaseC");


        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "urlrewriter",
                new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("namespace/urlrewriter.cnd"))));

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                UpdaterUtils.removeNode(node, "urlrewriter");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-basedocument");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-rulecondition");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-advancedrule");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-rule");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-xmlrule");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-ruleset");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-static-dropdown");
                UpdaterUtils.removeNode(node, "urlrewriter-namespace-ajax-aware-string");
                UpdaterUtils.removeNode(node, "configuration-frontend-cms-browser-navigator");
                UpdaterUtils.removeNode(node, "configuration-frontend-cms-browser-urlrewritertreeloader");
                UpdaterUtils.removeNode(node, "configuration-frontend-cms-pickers-folders-navigator");
                UpdaterUtils.removeNode(node, "configuration-frontend-cms-pickers-folders-urlrewritertreeloader");
                UpdaterUtils.removeNode(node, "configuration-frontend-cms-static-configtranslator-urlrewriter");
                UpdaterUtils.removeNode(node, "configuration-frontend-cms-treeviews-urlrewriter");
                UpdaterUtils.removeNode(node, "configuration-frontend-cms-folder-views-ruleset");
                UpdaterUtils.removeNode(node, "urlrewriter-root-content");
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:frontend/cms") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                UpdaterUtils.removeNode(node, "cms-browser/urlrewriterTreeLoader");
                UpdaterUtils.removeNode(node, "cms-pickers/folders/urlrewriterTreeLoader");
                UpdaterUtils.removeNode(node, "cms-static/configTranslator/hippostd:translations/section-urlrewriter");
                UpdaterUtils.removeNode(node, "cms-tree-views/urlrewriter");
                UpdaterUtils.removeNode(node, "cms-folder-views/urlrewriter:ruleset");
            }
        });

        //remove whole namespace
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:namespaces") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                UpdaterUtils.removeNode(node, "urlrewriter");
            }
        });

    }


}
