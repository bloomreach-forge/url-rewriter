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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This updater removes old urlrewriter:ruleset nodes (in template queries), reloads the queries and transforms
 * any existing urlrewriter directories to urlrewriter:ruleset
 *
 * @version $Id$
 */
public class UrlRewriter_1_04_UpdaterPhaseD implements UpdaterModule {


    protected static final Logger log = LoggerFactory.getLogger(UrlRewriter_1_04_UpdaterPhaseD.class);

    public void register(final UpdaterContext context) {

        context.registerName("url-rewriter-updater-1-04-phaseD");
        context.registerStartTag("url-rewriter-updater-1-04-phaseC");
        context.registerEndTag("url-rewriter-updater-1-04-phaseD");

        // visit hippo:initialize
        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:initialize") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                UpdaterUtils.removeNode(node, "new-urlrewriter-document");
                UpdaterUtils.removeNode(node, "new-urlrewriter-folder");
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/hippo:configuration/hippo:queries/hippo:templates") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                UpdaterUtils.removeNode(node, "new-urlrewriter-document");
                UpdaterUtils.removeNode(node, "new-urlrewriter-folder");
            }
        });

        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/content/urlrewriter") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                    UpdaterUtils.changeDirectoriesToRulesets(children.nextNode());
                }
            }
        });

    }
}
