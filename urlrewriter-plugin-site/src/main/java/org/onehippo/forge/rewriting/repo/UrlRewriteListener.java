/**
 * Copyright (C) 2011 - 2012 Hippo
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

import javax.jcr.observation.Event;

import org.hippoecm.hst.core.jcr.GenericEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * EventListener invalidating a {@link RewritingManager}.
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