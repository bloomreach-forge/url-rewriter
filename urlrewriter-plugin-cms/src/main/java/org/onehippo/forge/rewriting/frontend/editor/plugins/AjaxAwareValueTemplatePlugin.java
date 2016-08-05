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

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.hippoecm.frontend.editor.plugins.ValueTemplatePlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AjaxAwareValueTemplatePlugin extends ValueTemplatePlugin implements AjaxUpdatesAware {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(AjaxAwareValueTemplatePlugin.class);
    private static final List<String> nameMustBeSpecifiedFor = Arrays.asList("header", "attribute", "cookie", "parameter", "session-attribute");
    private final AttributeModifier disabledAttributeModifier = new AttributeModifier("disabled", new Model<>("disabled"));
    private final AttributeModifier placeholderAttributeModifier = new AttributeModifier("placeholder",
            new ClassResourceModel("not-applicable-placeholder", AjaxAwareValueTemplatePlugin.class, null));

    @Override
    public void onAjaxUpdate(AjaxRequestTarget target, RenderPlugin invoker) {

        final Object conditionType = invoker.getModelObject();
        this.visitChildren(new IVisitor<Component, IVisit>() {
            @Override
            public void component(final Component component, final IVisit visit) {
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
                            if (!component.getBehaviors().contains(disabledAttributeModifier)) {
                                component.add(disabledAttributeModifier);
                            }
                            if (!component.getBehaviors().contains(placeholderAttributeModifier)) {
                                component.add(placeholderAttributeModifier);
                            }
                            AjaxAwareValueTemplatePlugin.this.setModelObject("");
                            AjaxAwareValueTemplatePlugin.this.redraw();
                        }
                    }
                }
            }
        });
        target.add(this);
    }

    public AjaxAwareValueTemplatePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        this.setOutputMarkupId(true);
    }
}
