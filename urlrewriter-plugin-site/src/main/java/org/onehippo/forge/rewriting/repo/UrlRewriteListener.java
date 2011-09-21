/*
 *  Copyright 2010 Hippo.
 * 
 */
package org.onehippo.forge.rewriting.repo;

import javax.jcr.observation.Event;

import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EventListener invalidating a PropertiesManager.
 */
public class UrlRewriteListener extends GenericEventListener {

    private Logger log = LoggerFactory.getLogger(UrlRewriteListener.class);

    private RewritingManager rewriteManager;

    public void setRewriteManager(RewritingManager rewriteManager) {
        this.rewriteManager = rewriteManager;
    }

    protected void onNodeAdded(Event event) {
        doInvalidation(event);
    }

    protected void onNodeRemoved(Event event) {
        doInvalidation(event);
    }

    protected void onPropertyAdded(Event event) {
        doInvalidation(event);
    }

    protected void onPropertyChanged(Event event) {
        doInvalidation(event);
    }

    protected void onPropertyRemoved(Event event) {
        doInvalidation(event);
    }

    private void doInvalidation(final Event path) {
        // TODO enable for fine grained solution
        /*
        String docPath = (path.lastIndexOf('/') < 0) ? path : path.substring(0, path.lastIndexOf('/'));
        if (docPath.endsWith("]") && (docPath.lastIndexOf('[') > -1)) {
            docPath = docPath.substring(0, docPath.lastIndexOf('['));
        }*/

        rewriteManager.invalidate(path);
    }
}