
<!doctype html>
<%@ include file="/WEB-INF/jspf/htmlTags.jspf" %>
<%@ page isErrorPage="true" %>
<% response.setStatus(404); %>

<html lang="en">
<head>
  <meta charset="utf-8"/>
  <title>404 error</title>
</head>
<body>
<h2>Welcome to URL Rewriter demo</h2>
<p>
  This is an empty Hippo site from the archetype. There is nothing to show on the site yet; normally
  <a href="http://<%=request.getServerName() + ':' + request.getServerPort() + "/essentials"%>" target="_blank">Hippo's setup application</a>
  is used to develop your project.
</p>
<p>
  <strong>However</strong>, this is a URL Rewriter demo and it does not need any running HST site because the
  <code>org.onehippo.forge.rewriting.HippoRewriteFilter</code> kicks in before the HST!
  To demo the URL rewriting some documents are set up in the CMS:
</p>
<ul>
  <li>An regular rule document redirecting <a href="about">about</a> to http://www.onehippo.com</li>
  <li>An advanced rule document redirecting <a href="contact">contact</a> to http://www.onehippo.org</li>
  <li>An XML rule document redirecting <a href="test/status">test/status</a> /rewrite-status (see below)</li>
</ul>
<p>The page <a href="rewrite-status">rewrite-status</a> shows active rules loaded into the underlying software from
  <a href="http://www.tuckey.org/urlrewrite/">Tuckey</a></p>
</body>
</html>
