<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.hippoecm.hst.core.container.ContainerSecurityException" %>
<%@ page import="org.hippoecm.hst.core.container.ContainerSecurityNotAuthenticatedException" %>
<%@ page import="org.hippoecm.hst.core.container.ContainerSecurityNotAuthorizedException" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://www.hippoecm.org/jsp/hst/core" prefix="hst"%>

<fmt:setBundle basename="org.hippoecm.hst.security.servlet.LoginServlet" />

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<%
String destination = (String) session.getAttribute("org.hippoecm.hst.security.servlet.destination");
if (destination == null) destination = "";

int autoRedirectSeconds = 2;

ContainerSecurityException securityException = (ContainerSecurityException) session.getAttribute("org.hippoecm.hst.security.servlet.exception");
boolean accessForbidden = (securityException instanceof ContainerSecurityNotAuthorizedException);

if (accessForbidden) {
    autoRedirectSeconds = 5;
}

session.invalidate();
%>

<c:set var="accessForbidden" value="<%=accessForbidden%>" />

<hst:link var="loginFormUrl" path="/login/form">
  <hst:param name="destination" value="<%=destination%>" />
</hst:link>
<hst:link var="loginProxyUrl" path="/login/proxy">
  <hst:param name="destination" value="<%=destination%>" />
</hst:link>

    <title>
      <c:choose>
        <c:when test="${accessForbidden}">
          <fmt:message key="label.access.forbidden" />
        </c:when>
        <c:otherwise>
          <fmt:message key="label.authen.required" />
        </c:otherwise>
      </c:choose>
    </title>
    <meta http-equiv='refresh' content='<%=autoRedirectSeconds%>;url=${loginFormUrl}' />
    <link rel="stylesheet" type="text/css" href="<hst:link path='/login/hst/security/skin/screen.css' />" />
    <link rel="stylesheet" media="screen" type="text/css" href="<hst:link path='/login/hst/security/skin/css/help-overlay.css'/>" />
  </head>
  <body class="hippo-root">
    <div>
      <div class="hippo-login-panel">
        <form class="hippo-login-panel-form" name="signInForm" method="post" action="${loginProxyUrl}">
          <h2><div class="hippo-global-hideme"><span>Hippo CMS 7</span></div></h2>
          <div class="hippo-login-form-container">
            <table>
              <tr>
                <td>
                  <p>
                    <c:choose>
                      <c:when test="${accessForbidden}">
                        <fmt:message key="message.access.forbidden">
                          <fmt:param value="<%=destination%>" />
                        </fmt:message>
                      </c:when>
                      <c:otherwise>
                        <fmt:message key="message.authen.required">
                          <fmt:param value="<%=destination%>" />
                        </fmt:message>
                      </c:otherwise>
                    </c:choose>
                  </p>
                </td>
              </tr>
              <tr>
                <td>
                  <p>
                    <a href="${loginFormUrl}"><fmt:message key="message.try.again"/></a>
                    <br/><br/>
                    <fmt:message key="message.page.auto.redirect.in.seconds">
                      <fmt:param value="<%=autoRedirectSeconds%>" />
                    </fmt:message>
                  </p>
                </td>
              </tr>
            </table>
          </div>
        </form>
        <div class="hippo-login-panel-copyright">
          &copy; 1999-2011 Hippo B.V.
        </div>
      </div>
    </div>
    <script src="<hst:link path='/login/hst/security/skin/js/jquery-1.4.2.min.js' />" type="text/javascript"></script>
    <script src="<hst:link path='/login/hst/security/skin/js/jquery.tools.min.js' />" type="text/javascript"></script>
    <script src="<hst:link path='/login/hst/security/skin/js/help-overlay.js' />" type="text/javascript"></script>
  </body>
</html>