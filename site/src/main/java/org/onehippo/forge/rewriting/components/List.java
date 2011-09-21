
package org.onehippo.forge.rewriting.components;

import org.onehippo.forge.rewriting.componentsinfo.ListInfo;
import org.hippoecm.hst.configuration.components.ParametersInfo;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ParametersInfo(type = ListInfo.class)
public class List extends BaseComponent {

    public static final Logger log = LoggerFactory.getLogger(List.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

       ListInfo info = getParametersInfo(request);
       HippoBean scopeBean = null;

       String scope = info.getScope();
       if(scope == null) {
           throw new HstComponentException("Scope is not allowed to be null for a List component. Cannot create a list");
       }

       scopeBean = getSiteContentBaseBean(request);
       if("".equals(scope) || "/".equals(scope)) {
           // the scope is the root content bean of this site, scopeBean is already ok.
       } else {
           // strip leading and trailing slashes
           scope = PathUtils.normalizePath(scope);
           scopeBean = scopeBean.getBean(scope);
           if(scopeBean == null) {
               throw new HstComponentException("Scope '"+scope+"' does not point to a bean for Mount with content path '"+request.getRequestContext().getResolvedMount().getMount().getContentPath()+"'. Cannot create a list");
           }
       }

       if(scope == null) {
           throw new HstComponentException("For an Overview component there must be a content bean available to search below. Cannot create an overview");
       }
       createAndExecuteSearch(request, info, scopeBean, null);
    }
}
