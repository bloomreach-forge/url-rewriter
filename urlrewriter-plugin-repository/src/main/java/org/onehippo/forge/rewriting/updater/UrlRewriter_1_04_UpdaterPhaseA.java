package org.onehippo.forge.rewriting.updater;

import java.io.BufferedInputStream;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.ext.UpdaterContext;
import org.hippoecm.repository.ext.UpdaterItemVisitor;
import org.hippoecm.repository.ext.UpdaterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This updater registers an intermediate cnd containing all the new document types. This is a trivial cnd change.
 * Then the updater moves all existing content of type urlrewriter:ruleset to urlrewriter:advancedrule.
 *
 * @version $Id$
 */
public class UrlRewriter_1_04_UpdaterPhaseA implements UpdaterModule {


    protected static final Logger log = LoggerFactory.getLogger(UrlRewriter_1_04_UpdaterPhaseA.class);

    public void register(final UpdaterContext context) {

        context.registerName("url-rewriter-updater-1-04-phaseA");
        context.registerStartTag("url-rewriter-updater-1-04");
        context.registerEndTag("url-rewriter-updater-1-04-phaseA");


        //Load the intermediate cnd
        context.registerVisitor(new UpdaterItemVisitor.NamespaceVisitor(context, "urlrewriter",
                new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("namespace/urlrewriter-transition.cnd"))));


        context.registerVisitor(new UpdaterItemVisitor.PathVisitor("/content/urlrewriter") {
            @Override
            protected void leaving(Node node, int level) throws RepositoryException {
                for (NodeIterator children = node.getNodes(); children.hasNext(); ) {
                    Node childNode = children.nextNode();
                    try {
                        UpdaterUtils.updateContentToNewUrlRewriterTypes(childNode);
                    } catch (RepositoryException e) {
                        log.error("Exception while processing node {}", childNode.getPath());
                    }

                }
            }
        });

    }
}
