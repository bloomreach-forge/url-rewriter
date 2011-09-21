
package org.onehippo.forge.rewriting.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

@Node(jcrType="urlrewriting:basedocument")
public class BaseDocument extends HippoDocument {

    public String getTitle() {
        return getProperty("urlrewriting:title");
    }
    
    public String getSummary() {
        return getProperty("urlrewriting:summary");
    }
}
