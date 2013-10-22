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
                        log.error("Exception while processing node {}", childNode.getPath());
                    }
                }
            }
        });

    }
}
