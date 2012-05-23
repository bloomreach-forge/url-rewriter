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
package org.onehippo.forge.rewriting.frontend.plugins.cms.browse.list.resolvers;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

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

    private transient String ruleName;
    private transient String originalUrl;
    private transient String rewriteUrl;
    private transient String rewriteType;

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
                    boolean isHistoric = false;
                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        /* TODO : What happens here exactly?
                        NodeIterator docs = node.getNodes(node.getName());
                        while (docs.hasNext()) {
                            document = docs.nextNode();
                            primaryType = document.getPrimaryNodeType();
                            if (document.isNodeType(HippoStdNodeType.NT_PUBLISHABLE)) {
                                String state = document.getProperty(HippoStdNodeType.HIPPOSTD_STATE).getString();
                                if ("unpublished".equals(state)) {
                                    break;
                                }
                            }
                        } */

                        //TODO Do something!!
                        document = node.getNode(node.getName());
                        
                    } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        document = node;
                        primaryType = document.getPrimaryNodeType();
                    } else if (node.isNodeType("nt:version")) {
                        /* TODO : What happens here exactly?
                        isHistoric = true;
                        Node frozen = node.getNode("jcr:frozenNode");
                        String primary = frozen.getProperty("jcr:frozenPrimaryType").getString();
                        NodeTypeManager ntMgr = frozen.getSession().getWorkspace().getNodeTypeManager();
                        primaryType = ntMgr.getNodeType(primary);
                        if (primaryType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            document = frozen;
                        }
                        */
                    }
                    if (document != null) {
                        /* TODO : What happens here exactly?
                        if (primaryType.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)
                                || document.isNodeType(HippoStdNodeType.NT_PUBLISHABLESUMMARY)) {
                            cssClass = StateIconAttributeModifier.PREFIX
                                    + (isHistoric ? "prev-" : "")
                                    + document.getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY).getString()
                                    + StateIconAttributeModifier.SUFFIX;
                            IModel stateModel = new JcrPropertyValueModel(new JcrPropertyModel(document
                                    .getProperty(HippoStdNodeType.HIPPOSTD_STATESUMMARY)));
                            summary = (String) new TypeTranslator(new JcrNodeTypeModel(
                                    HippoStdNodeType.NT_PUBLISHABLESUMMARY)).getValueName(
                                    HippoStdNodeType.HIPPOSTD_STATESUMMARY, stateModel).getObject();

                            if (primaryType.isNodeType(HIPPOSTDPUBWF_DOCUMENT) || document.isNodeType(HIPPOSTDPUBWF_DOCUMENT)) {
                                if (document.hasProperty(HIPPOSTDPUBWF_PUBLICATION_DATE)) {
                                    publicationDate = document.getProperty(HIPPOSTDPUBWF_PUBLICATION_DATE).getDate();
                                }
                                if(document.hasProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE)) {
                                    lastModifiedDate = document.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_DATE).getDate();
                                }
                                if(document.hasProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY)) {
                                    lastModifiedBy = document.getProperty(HIPPOSTDPUBWF_LAST_MODIFIED_BY).getString();
                                }
                                if(document.hasProperty(HIPPOSTDPUBWF_CREATION_DATE)) {
                                    creationDate = document.getProperty(HIPPOSTDPUBWF_CREATION_DATE).getDate();
                                }
                                if(document.hasProperty(HIPPOSTDPUBWF_CREATED_BY)) {
                                    createdBy = document.getProperty(HIPPOSTDPUBWF_CREATED_BY).getString();
                                }
                            }

                            observable.setTarget(new JcrNodeModel(document));
                        }*/


                        if (document.hasProperty("urlrewriter:rulefrom")) {
                            originalUrl = document.getProperty("urlrewriter:rulefrom").getString();
                        }

                        if (document.hasProperty("urlrewriter:ruleto")) {
                            rewriteUrl = document.getProperty("urlrewriter:ruleto").getString();
                        }

                        if (document.hasProperty("urlrewriter:ruletype")) {
                            rewriteType = document.getProperty("urlrewriter:ruletype").getString();
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