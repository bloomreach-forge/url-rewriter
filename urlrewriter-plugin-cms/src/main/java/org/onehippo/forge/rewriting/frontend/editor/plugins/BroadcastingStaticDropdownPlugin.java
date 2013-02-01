/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.rewriting.frontend.editor.plugins;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.CSSPackageResource;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.string.StringList;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.TextDiffModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO This could be extending StaticDropdownPlugin
public class BroadcastingStaticDropdownPlugin extends RenderPlugin<String> {

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(BroadcastingStaticDropdownPlugin.class);

    private List<String> options;

    public BroadcastingStaticDropdownPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        add(CSSPackageResource.getHeaderContribution(BroadcastingStaticDropdownPlugin.class, "BroadcastingStaticDropdownPlugin.css"));

        String mode = config.getString("mode", "view");
        Fragment fragment = new Fragment("fragment", mode, this);
        add(fragment);

        if ("edit".equals(mode)) {
            options = StringList.tokenize(config.getString("selectable.options", ""), ",").toList();
            DropDownChoice<String> choice = new DropDownChoice<String>("select", getModel(), options);
            choice.add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(final AjaxRequestTarget target) {
                    //TODO Using the RenderPlugin here is not very safe. The fact that the parent we're after is indeed a RenderPlugin is an assumption that simply holds true here. It would be better if org.hippoecm.frontend.plugin.impl.PluginFactory$LayoutPlugin was used instead, but it is private
                    BroadcastingStaticDropdownPlugin.this.findParent(RenderPlugin.class).visitChildren(new IVisitor<Component>() {
                        @Override
                        public Object component(final Component component) {
                            if (component instanceof AjaxUpdatesAware) {
                                ((AjaxUpdatesAware) component).onAjaxUpdate(target, BroadcastingStaticDropdownPlugin.this);
                            }
                            return null;
                        }
                    });
                }
            });

            fragment.add(choice);
        } else {
            Label label = null;
            if ("compare".equals(mode)) {
                if (config.containsKey("model.compareTo")) {
                    IModelReference<String> baseRef = context.getService(config.getString("model.compareTo"),
                            IModelReference.class);
                    if (baseRef != null) {
                        IModel<String> baseModel = baseRef.getModel();
                        if (baseModel == null) {
                            log.info("base model service provides null model");
                            baseModel = new Model<String>(null);
                        }
                        label = (Label) new Label("selectLabel", new TextDiffModel(baseModel, getModel()))
                                .setEscapeModelStrings(false);
                    } else {
                        log.warn("opened in compare mode, but no base model service is available");
                    }
                } else {
                    log.warn("opened in compare mode, but no base model was configured");
                }
            }
            if (label == null) {
                label = new Label("selectLabel", getModel());
            }
            fragment.add(label);
        }
    }

}
