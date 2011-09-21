<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<ul>
<c:forEach var="componentException" items="${errorComponentWindow.componentExceptions}">
  <li>${componentException.message}</li>
</c:forEach>
</ul>
