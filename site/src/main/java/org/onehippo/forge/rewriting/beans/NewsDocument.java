
package org.onehippo.forge.rewriting.beans;

import java.util.Calendar;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean;

@Node(jcrType="urlrewriting:newsdocument")
public class NewsDocument extends TextDocument{

    public Calendar getDate() {
        return getProperty("urlrewriting:date");
    }

    /**
     * Get the imageset of the newspage
     *
     * @return the imageset of the newspage
     */
    public HippoGalleryImageSetBean getImage() {
        return getLinkedBean("urlrewriting:image", HippoGalleryImageSetBean.class);
    }
    
}
