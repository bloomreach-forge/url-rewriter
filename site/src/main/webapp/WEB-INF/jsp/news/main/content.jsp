
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>

<c:if test="${not empty document.title}">
  <hst:element var="headTitle" name="title">
    <c:out value="${document.title}"/>
  </hst:element>
  <hst:headContribution keyHint="headTitle" element="${headTitle}" />
</c:if>


<h2>${document.title}</h2>
<c:if test="${hst:isReadable(document, 'date')}">
   <p><fmt:formatDate value="${document.date.time}" type="Date"/></p>
</c:if>
<p>${document.summary}</p>
<hst:html hippohtml="${document.html}"/>
<c:if test="${hst:isReadable(document, 'image')}">
  <hst:link var="img" hippobean="${document.image.original}"/>
  <p>
  <br/>
  <img src="${img}" title="${document.image.fileName}" alt="${document.image.fileName}"/>
  <br/>
  ${document.image.description}
</c:if>
</p>


