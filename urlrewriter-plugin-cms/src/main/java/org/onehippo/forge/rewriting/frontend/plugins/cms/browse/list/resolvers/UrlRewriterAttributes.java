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
package org.onehippo.forge.rewriting.frontend.plugins.cms.browse.list.resolvers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.wicket.model.IDetachable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.Observable;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class UrlRewriterAttributes implements IObservable, IDetachable {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(UrlRewriterAttributes.class);

    private JcrNodeModel nodeModel;
    private Observable observable;
    private transient boolean loaded = false;

    private transient String originalUrl;
    private transient String rewriteUrl;
    private transient String rewriteType;
    private transient Boolean hasConditions;

    public UrlRewriterAttributes(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
        observable = new Observable(nodeModel);
    }

    public String getOriginalUrl() {
        load();
        return originalUrl;
    }

    public String getRewriteUrl() {
        load();
        return rewriteUrl;
    }

    public String getRewriteType() {
        load();
        return rewriteType;
    }

    public Boolean hasConditions() {
        load();
        return hasConditions;
    }

    public void detach() {
        loaded = false;

        originalUrl = null;

        nodeModel.detach();
        observable.detach();
    }

    void load() {
        if (!loaded) {
            observable.setTarget(null);
            try {
                Node node = nodeModel.getNode();
                if (node != null) {
                    Node document = null;
                    NodeType primaryType = null;
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        NodeIterator docs = node.getNodes(node.getName());
                        while (docs.hasNext()) {
                            document = docs.nextNode();
                            primaryType = document.getPrimaryNodeType();
                            //TODO Replace when the constant is added to the api
                            if (document.isNodeType("hippostd:publishable")) {
                                String state = document.getProperty("hippostd:state").getString();
                                if ("unpublished".equals(state)) {
                                    break;
                                }
                            }
                        }
                    } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        document = node;
                        primaryType = document.getPrimaryNodeType();
                    } else if (node.isNodeType("nt:version")) {
                        Node frozen = node.getNode("jcr:frozenNode");
                        String primary = frozen.getProperty("jcr:frozenPrimaryType").getString();
                        NodeTypeManager ntMgr = frozen.getSession().getWorkspace().getNodeTypeManager();
                        primaryType = ntMgr.getNodeType(primary);
                        if (primaryType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            document = frozen;
                        }
                    }
                    if (document != null) {
                        //TODO Replace when the constant is added to the api
                        if (primaryType.isNodeType("hippostd:publishableSummary") || document.isNodeType("hippostd:publishableSummary")) {
                            observable.setTarget(new JcrNodeModel(document));
                        }

                        if (document.hasProperty("urlrewriter:rulefrom")) {
                            originalUrl = document.getProperty("urlrewriter:rulefrom").getString();
                        }

                        if (document.hasProperty("urlrewriter:ruleto")) {
                            rewriteUrl = document.getProperty("urlrewriter:ruleto").getString();
                        }

                        if (document.hasProperty("urlrewriter:ruletype")) {
                            rewriteType = document.getProperty("urlrewriter:ruletype").getString();
                        }

                        if (primaryType.isNodeType("urlrewriter:advancedrule")){
                            hasConditions = document.hasNode("urlrewriter:rulecondition");
                        }
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Unable to obtain state properties", ex);
            }
            loaded = true;
        }
    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        observable.setObservationContext(context);
    }

    public void startObservation() {
        observable.startObservation();
    }

    public void stopObservation() {
        observable.stopObservation();
    }


}
