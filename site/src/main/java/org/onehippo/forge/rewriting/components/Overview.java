
package org.onehippo.forge.rewriting.components;

import org.onehippo.forge.rewriting.componentsinfo.PageableListInfo;
import org.hippoecm.hst.configuration.components.ParametersInfo;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ParametersInfo(type = PageableListInfo.class)
public class Overview extends BaseComponent {

    public static final Logger log = LoggerFactory.getLogger(Overview.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {

       PageableListInfo info = getParametersInfo(request);
       HippoBean scope = getContentBean(request);

       if(scope == null) {
           throw new HstComponentException("For an Overview component there must be a content bean available to search below. Cannot create an overview");
       }
       createAndExecuteSearch(request, info, scope, null);
    }
}

