
package org.onehippo.forge.rewriting.components;

import org.onehippo.forge.rewriting.componentsinfo.SearchInfo;
import org.hippoecm.hst.configuration.components.ParametersInfo;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ParametersInfo(type = SearchInfo.class)
public class Search extends BaseComponent {

    public static final Logger log = LoggerFactory.getLogger(Search.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

       SearchInfo info = getParametersInfo(request);
       HippoBean scope = getSiteContentBaseBean(request);

       String query = getPublicRequestParameter(request, "query");
       if(query == null) {
           // test namespaced query parameter
           query = request.getParameter("query");
       }
       createAndExecuteSearch(request, info, scope, query);
    }
}
