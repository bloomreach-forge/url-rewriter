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
 * This updater removes all old urlrewriter:rule and urlrewriter:rulecondition nodes.
 *
 *
 * @version $Id$
 */
public class UrlRewriter_1_04_UpdaterPhaseB implements UpdaterModule {


    protected static final Logger log = LoggerFactory.getLogger(UrlRewriter_1_04_UpdaterPhaseB.class);

    public void register(final UpdaterContext context) {

        context.registerName("url-rewriter-updater-1-04-phaseB");
        context.registerStartTag("url-rewriter-updater-1-04-phaseA");
        context.registerEndTag("url-rewriter-updater-1-04-phaseB");


        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/content/urlrewriter") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                    Node childNode = children.nextNode();
                    try {
                        UpdaterUtils.removeOldRewriterRules(childNode);
                    } catch (RepositoryException e) {
                        log.error("Exception while processing node {}", childNode.getPath(), e);
                    }
                }
            }
        });

    }
}
