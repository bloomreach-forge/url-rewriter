<%@ page contentType="text/html; charset=UTF-8" language="java"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>

<c:if test="${not empty document.title}">
  <hst:element var="headTitle" name="title">
    <c:out value="${document.title}"/>
  </hst:element>
  <hst:headContribution keyHint="headTitle" element="${headTitle}" />
</c:if>


<h2>${document.title}</h2>
<p>I18n key example: <fmt:message key="home.title"/></p>
<p>${document.summary}</p>
<hst:html hippohtml="${document.html}"/>


