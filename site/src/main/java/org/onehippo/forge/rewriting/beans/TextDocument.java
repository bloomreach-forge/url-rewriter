
package org.onehippo.forge.rewriting.beans;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoHtml;

@Node(jcrType="urlrewriting:textdocument")
public class TextDocument extends BaseDocument{

    public HippoHtml getHtml(){
        return getHippoHtml("urlrewriting:body");    
    }
}
