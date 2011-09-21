
package org.onehippo.forge.rewriting.components;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.forge.rewriting.componentsinfo.GeneralListInfo;
import org.onehippo.forge.rewriting.componentsinfo.PageableListInfo;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseComponent extends BaseHstComponent {

    public static final Logger log = LoggerFactory.getLogger(BaseComponent.class);


    /**
     * Creates and executes a search, and puts a {@link HstQueryResult}, {@link PageableListInfo}, crPage, query and optionally a {@link List<Integer>} of pages on the
     * request
     * @param request
     * @param info
     * @param scope the scope to search below.
     * @param query the free text query to search for. If <code>null</code> or empty, it will be ignored
     */

    protected void createAndExecuteSearch(HstRequest request, GeneralListInfo info, HippoBean scope, String query) throws HstComponentException {
       if(scope == null) {
           throw new HstComponentException("Scope is not allowed to be null for a search");
       }
       int pageSize = info.getPageSize();
       if(pageSize == 0) {
           log.warn("Empty pageSize or set to null. This is not a valid size. Use default size");

       }
       String docType = info.getDocType();
       String sortBy = info.getSortBy();
       String sortOrder = info.getSortOrder();
       String crPageStr = request.getParameter("page");


       int crPage = 1;
       if(crPageStr != null) {
           try {
               crPage = Integer.parseInt(crPageStr);
           } catch (NumberFormatException e) {
               throw new HstComponentException("Invalid page number '"+crPage+"'");
           }
       }

       @SuppressWarnings("unchecked")
       Class filterClass = getObjectConverter().getAnnotatedClassFor(docType);
       if(filterClass == null) {
           throw new HstComponentException("There is no bean for docType '"+docType+"'. Cannot use '"+docType+"' as in this search");
       }

        try {
            @SuppressWarnings("unchecked")
            HstQuery hstQuery = getQueryManager().createQuery(scope, filterClass, true);
            hstQuery.setLimit(pageSize);
            hstQuery.setOffset(pageSize * (crPage - 1));
            if(sortBy != null && !"".equals(sortBy)) {
                if(sortOrder == null || "".equals(sortOrder) || "descending".equals(sortOrder)) {
                    hstQuery.addOrderByDescending(sortBy);
                } else {
                    hstQuery.addOrderByAscending(sortBy);
                }
            }

            if(query != null && !"".equals(query)) {
                Filter f = hstQuery.createFilter();
                f.addContains(".", query);
                hstQuery.setFilter(f);
            }

            HstQueryResult result = hstQuery.execute();

            request.setAttribute("result", result);
            request.setAttribute("info", info);
            request.setAttribute("crPage", crPage);
            request.setAttribute("query", query);


            if(info instanceof PageableListInfo && ((PageableListInfo)info).isPagesVisible()) {
                request.setAttribute("totalSize", result.getTotalSize());
                // add pages
                if(result.getTotalSize() > pageSize) {
                    List<Integer> pages = new ArrayList<Integer>();
                    int numberOfPages = result.getTotalSize() / pageSize ;
                    if(result.getTotalSize() % pageSize != 0) {
                        numberOfPages++;
                    }
                    for(int i = 0; i < numberOfPages; i++) {
                        pages.add(i + 1);
                    }
                    request.setAttribute("pages", pages);
                }
            }


        } catch (QueryException e) {
            throw new HstComponentException("Exception occured during creation or execution of HstQuery. ", e);
        }
    }

}
