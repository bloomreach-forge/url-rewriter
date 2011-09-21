
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix="hst" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:if test="${not empty info.title}">
  <hst:element var="headTitle" name="title">
    <c:out value="${info.title}"/>
  </hst:element>
  <hst:headContribution keyHint="headTitle" element="${headTitle}" />
</c:if>


<h2>${info.title}. <c:if test="${not empty totalSize}">Total results ${totalSize}</c:if></h2>
<ul>
  <c:forEach var="item" items="${result.hippoBeans}">
    <hst:link var="link" hippobean="${item}"/>
    <li>
      <a href="${link}">${item.title}</a>
      <div>
        <c:if test="${hst:isReadable(item, 'date')}">
        <p><fmt:formatDate value="${item.date.time}" type="Date" pattern="MMMM d, yyyy h:mm a" /></p>
        </c:if>
        <p>${item.summary}</p>
      </div>
    </li>
  </c:forEach>
</ul>
<c:if test="${fn:length(pages) gt 0}">
  <ul id="paging-nav">
    <c:forEach var="page" items="${pages}">
      <c:set var="active" value="" />
      <c:choose>
        <c:when test="${crPage == page}">
           <li>${page}</li>
        </c:when>
        <c:otherwise>
          <hst:renderURL var="pagelink">
            <hst:param name="page" value="${page}" />
            <hst:param name="query" value="${query}" />
          </hst:renderURL>
          <li><a href="${pagelink}" title="${page}">${page}</a></li>
        </c:otherwise>
      </c:choose>
    </c:forEach>
  </ul>
</c:if>


