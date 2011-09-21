
package org.onehippo.forge.rewriting.componentsinfo;

import org.hippoecm.hst.configuration.components.Parameter;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentWrapper;


public interface GeneralListInfo  {

    @Parameter(name = "title",defaultValue="Overview", displayName = "The title of the page", typeHint = ComponentWrapper.ParameterType.STRING)
    String getTitle();

    @Parameter(name = "pageSize", defaultValue="10", typeHint = ComponentWrapper.ParameterType.NUMBER, displayName = "Page Size")
    int getPageSize();

    @Parameter(name = "docType", defaultValue="urlrewriting:basedocument", displayName = "Document Type", typeHint = ComponentWrapper.ParameterType.STRING)
    String getDocType();

    @Parameter(name = "sortBy", displayName = "Sort By Property", typeHint = ComponentWrapper.ParameterType.STRING)
    String getSortBy();

    @Parameter(name = "sortOrder",defaultValue="descending", displayName = "Sort Order", typeHint = ComponentWrapper.ParameterType.STRING)
    String getSortOrder();
}
