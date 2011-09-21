
<%@ page language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/xml" prefix="x" %>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix='hst'%>


<ul class="sitenav">
  <c:forEach var="item" items="${menu.siteMenuItems}">
      <li>
          <c:choose >
              <c:when test="${item.selected}">
                  <b>${item.name}</b>
              </c:when>
              <c:otherwise>
                  <hst:link var="link" link="${item.hstLink}"/>
                  <c:if test="${empty link}">
                    <c:set var="link" value="${item.externalLink}"/>
              </c:if>
                  <a href="${link}">
                      ${item.name}
                  </a>
              </c:otherwise>
          </c:choose>
          <c:if test="${item.expanded and not empty item.childMenuItems}">
          <ul>
              <c:forEach var="subitem" items="${item.childMenuItems}">
                  <c:choose >
                  <c:when test="${subitem.selected}">
                      <li>
                      <b>${subitem.name}</b>
                      </li>
                  </c:when>
                  <c:otherwise>
                      <hst:link var="link" link="${subitem.hstLink}"/>
                      <li>
                      <c:if test="${empty link}">
                        <c:set var="link" value="${subitem.externalLink}"/>
                     </c:if>
                      <a href="${link}">
                          ${subitem.name}
                      </a>
                      </li>
                  </c:otherwise>
                  </c:choose>
                      <c:if test="${subitem.expanded and not empty subitem.childMenuItems}">
                      <ul>
                      <c:forEach var="subsubitem" items="${subitem.childMenuItems}">
                          <c:choose >
                          <c:when test="${subsubitem.selected}">
                              <li>
                              <b>${subsubitem.name}</b>
                              </li>
                          </c:when>
                          <c:otherwise>
                              <hst:link var="link" link="${subsubitem.hstLink}"/>
                              <li>
                              <c:if test="${empty link}">
                                <c:set var="link" value="${subsubitem.externalLink}"/>
                              </c:if>
                              <a href="${link}">
                                  ${subsubitem.name}
                              </a>
                              </li>
                          </c:otherwise>
                          </c:choose>
                      </c:forEach>
                      </ul>
                      </c:if>
              </c:forEach>
          </ul>
          </c:if>
  </li>
  </c:forEach>
</ul>
