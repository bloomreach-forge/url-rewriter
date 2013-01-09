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
package org.hippoecm.frontend.editor.plugins;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.forge.AjaxUpdatesAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class AjaxAwareValueTemplatePlugin extends ValueTemplatePlugin implements AjaxUpdatesAware {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AjaxAwareValueTemplatePlugin.class);
    private static final List<String> nameMustBeSpecifiedFor = Arrays.asList("header", "attribute", "cookie", "parameter", "session-attribute");
    private static final AttributeModifier disabledAttributeModifier = new AttributeModifier("disabled", true, new Model<String>("disabled"));
    private static final AttributeModifier placeholderAttributeModifier = new AttributeModifier("placeholder", true, new Model<String>("Not applicable"));
    private PluginRequestTarget pluginRequestTarget;

    @Override
    public void onAjaxUpdate(AjaxRequestTarget target, RenderPlugin invoker) {

        final Object conditionType = invoker.getModelObject();
        this.visitChildren(new IVisitor<Component>() {
            @Override
            public Object component(Component component) {
                if (component.getId().equals("widget")) {
                    if(conditionType instanceof String){
                        if (nameMustBeSpecifiedFor.contains(conditionType)) {
                            if (component.getBehaviors().contains(disabledAttributeModifier)) {
                                component.remove(disabledAttributeModifier);
                            }
                            if (component.getBehaviors().contains(placeholderAttributeModifier)) {
                                component.remove(placeholderAttributeModifier);
                            }
                            AjaxAwareValueTemplatePlugin.this.redraw();
                        } else {
                            if (! component.getBehaviors().contains(disabledAttributeModifier)) {
                                component.add(disabledAttributeModifier);
                            }
                            if (!component.getBehaviors().contains(placeholderAttributeModifier)) {
                                component.add(placeholderAttributeModifier);
                            }
                            AjaxAwareValueTemplatePlugin.this.setModelObject("");
                            AjaxAwareValueTemplatePlugin.this.redraw();
                        }
                    }
                    return component;
                }
                return null;
            }
        });
        target.addComponent(this);
    }

    public AjaxAwareValueTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        this.setOutputMarkupId(true);
    }
}
