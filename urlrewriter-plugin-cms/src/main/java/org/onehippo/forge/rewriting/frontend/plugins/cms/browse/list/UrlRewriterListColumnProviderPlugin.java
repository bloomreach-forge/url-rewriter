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
package org.onehippo.forge.rewriting.frontend.plugins.cms.browse.list;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.onehippo.forge.rewriting.frontend.plugins.cms.browse.list.comparators.UrlRewriterAttributeComparator;
import org.onehippo.forge.rewriting.frontend.plugins.cms.browse.list.resolvers.UrlRewriterAttributeRenderer;
import org.onehippo.forge.rewriting.frontend.plugins.cms.browse.list.resolvers.UrlRewriterAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class UrlRewriterListColumnProviderPlugin  extends AbstractListColumnProviderPlugin {

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(UrlRewriterListColumnProviderPlugin.class);

    public UrlRewriterListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public IHeaderContributor getHeaderContributor() {
        return CSSPackageResource.getHeaderContribution(UrlRewriterListColumnProviderPlugin.class, "style.css");
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        return new ArrayList<ListColumn<Node>>();
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getColumns();
        ListColumn<Node> column;

        //Original Url
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-original-url", getClass()), "original-url");
        column.setComparator(new UrlRewriterAttributeComparator() {
            private static final long serialVersionUID = -4617312936280189361L;

            @Override
            protected int compare(UrlRewriterAttributes a1, UrlRewriterAttributes a2) {
                return UrlRewriterListColumnProviderPlugin.this.compare(a1.getOriginalUrl(), a2.getOriginalUrl());
            }
        });
        column.setCssClass("doclisting-original-url");
        column.setRenderer(new UrlRewriterAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(UrlRewriterAttributes attributes) {
                return attributes.getOriginalUrl();
            }
        });
        columns.add(column);


        //Rewrite Url
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-rewrite-url", getClass()), "rewrite-url");
        column.setComparator(new UrlRewriterAttributeComparator() {
            private static final long serialVersionUID = -4617312936280189361L;

            @Override
            protected int compare(UrlRewriterAttributes a1, UrlRewriterAttributes a2) {
                return UrlRewriterListColumnProviderPlugin.this.compare(a1.getRewriteUrl(), a2.getRewriteUrl());
            }
        });
        column.setCssClass("doclisting-rewrite-url");
        column.setRenderer(new UrlRewriterAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(UrlRewriterAttributes attributes) {
                return attributes.getRewriteUrl();
            }
        });
        columns.add(column);


        //Rewrite Type
        column = new ListColumn<Node>(new ClassResourceModel("doclisting-rewrite-type", getClass()), "rewrite-type");
        column.setComparator(new UrlRewriterAttributeComparator() {
            private static final long serialVersionUID = -4617312936280189361L;

            @Override
            protected int compare(UrlRewriterAttributes a1, UrlRewriterAttributes a2) {
                return UrlRewriterListColumnProviderPlugin.this.compare(a1.getRewriteType(), a2.getRewriteType());
            }
        });
        column.setCssClass("doclisting-rewrite-type");
        column.setRenderer(new UrlRewriterAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(UrlRewriterAttributes attributes) {
                return attributes.getRewriteType();
            }
        });
        columns.add(column);

        return columns;
    }

    protected int compare(String s1, String s2){
        if (s1 == null) {
            if (s2 == null) {
                return 0;
            }
            return 1;
        } else if (s2 == null) {
            return -1;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
    }
}
