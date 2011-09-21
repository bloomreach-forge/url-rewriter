
package org.onehippo.forge.rewriting.componentsinfo;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;


public interface ListInfo extends GeneralListInfo {

    /**
     * Returns the scope to search below. Leading and trailing slashes do not have meaning and will be skipped when using the scope. The scope
     * is always relative to the current {@link Mount#getContentPath()}, even if it starts with a <code>/</code>
     * @return the scope to search below
     */
    @Parameter(name = "scope",defaultValue="/", displayName = "Scope", typeHint = ComponentWrapper.ParameterType.STRING)
    String getScope();

    @Parameter(name = "cssclass",defaultValue="lightgrey", displayName = "Css Class", typeHint = ComponentWrapper.ParameterType.STRING)
    String getCssClass();
    
    @Parameter(name = "bgcolor",defaultValue="", displayName = "Background Color", typeHint = ComponentWrapper.ParameterType.COLOR)
    String getBgColor();
}
