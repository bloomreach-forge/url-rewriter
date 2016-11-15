/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.rewriting.run;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.ProxyHost;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

/**
 * This class is a Hippo version of Paul Tuckey's org.tuckey.web.filters.urlrewrite.RequestProxy
 * taken from release 4.0.4 on https://github.com/paultuckey/urlrewritefilter.
 *
 * Full URL (october 2016):
 * https://github.com/paultuckey/urlrewritefilter/blob/urlrewritefilter-4.0.4/src/main/java/org/tuckey/web/filters/urlrewrite/RequestProxy.java
 *
 * The class is rewritten to be used in an XML &lt;run&gt; element to have cookies passed though a proxy.
 *
 * The Hippo customizations are:
 * - take the target from XML configuration and init() method instead of execute() method parameter
 * - make the execute() method non-static, to be called by the run XML element
 * - use org.slf4j.Logger instead of org.tuckey.web.filters.urlrewrite.utils.Log
 *
 * Usage:
 *   &lt;![CDATA[<rule>
 *      <name>XML custom proxy</name>
 *      <from>^/test/(.*)$</from>
 *      <set name="wildcard">$1</set>
 *      <run class="org.onehippo.forge.rewriting.run.RequestProxyPassCookies" method="execute">
 *        <init-param>
 *          <param-name>target</param-name>
 *          <param-value>http://localhost:8081/$1</param-value>
 *         </init-param>
 *      </run>
 *      <to>null</to>
 *    </rule>]]&gt;
 */
public class RequestProxyPassCookies {
    private static final Logger log = LoggerFactory.getLogger(RequestProxyPassCookies.class);

    /**
     * Constructor
     */
    public RequestProxyPassCookies() {
        log.info("Instantiated {}", this);
    }

    private String target;

    /**
     * Init method is called by the org.tuckey.web.filters.urlrewrite.Run class
     *
     */
    public void init(ServletConfig config) {
        target = config.getInitParameter("target");
        log.info("Initializing parameter 'target' to '{}'", target);
    }

    /**
     * This method performs the proxying of the request to the target address.
     *
     * @param hsRequest  The request data which should be send to the
     * @param hsResponse The response data which will contain the data returned by the proxied request to target.
     * @throws IOException Passed on from the connection logic.
     */
    public void execute(final HttpServletRequest hsRequest, final HttpServletResponse hsResponse) throws IOException {

        // Hippo customization: replace wildcard in target
        final String wildcard = hsRequest.getAttribute("wildcard").toString();
        target = target.replace("$1", wildcard);

        log.info("execute, target is {}, wildcard is {}; response commit state: { }", target, wildcard, hsResponse.isCommitted());

        if (StringUtils.isBlank(target)) {
            log.error("The target address is not given. Please provide a target address.");
            return;
        }

        log.info("checking url");
        final URL url;
        try {
            url = new URL(target);
        } catch (MalformedURLException e) {
            log.error("The provided target url is not valid.", e);
            return;
        }


        log.info("seting up the host configuration");

        final HostConfiguration config = new HostConfiguration();

        ProxyHost proxyHost = getUseProxyServer((String) hsRequest.getAttribute("use-proxy"));
        if (proxyHost != null) config.setProxyHost(proxyHost);

        final int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
        config.setHost(url.getHost(), port, url.getProtocol());

        log.info("config is " + config.toString());

        final HttpMethod targetRequest = setupProxyRequest(hsRequest, url);
        if (targetRequest == null) {
            log.error("Unsupported request method found: " + hsRequest.getMethod());
            return;
        }

        //perform the reqeust to the target server
        final HttpClient client = new HttpClient(new SimpleHttpConnectionManager());
        if (log.isInfoEnabled()) {
            log.info("client state {}; client params {}; executeMethod / fetching data ...", client.getState(), client.getParams().toString());
        }

        final int result;
        if (targetRequest instanceof EntityEnclosingMethod) {
            final RequestProxyCustomRequestEntity requestEntity = new RequestProxyCustomRequestEntity(
                    hsRequest.getInputStream(), hsRequest.getContentLength(), hsRequest.getContentType());
            final EntityEnclosingMethod entityEnclosingMethod = (EntityEnclosingMethod) targetRequest;
            entityEnclosingMethod.setRequestEntity(requestEntity);
            result = client.executeMethod(config, entityEnclosingMethod);

        } else {
            result = client.executeMethod(config, targetRequest);
        }

        //copy the target response headers to our response
        setupResponseHeaders(targetRequest, hsResponse);

        InputStream originalResponseStream = targetRequest.getResponseBodyAsStream();
        //the body might be null, i.e. for responses with cache-headers which leave out the body
        if (originalResponseStream != null) {
            OutputStream responseStream = hsResponse.getOutputStream();
            copyStream(originalResponseStream, responseStream);
        }

        log.info("set up response, result code was " + result);
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        int count;
        while ((count = in.read(buf)) != -1) {
            out.write(buf, 0, count);
        }
    }


    public static ProxyHost getUseProxyServer(String useProxyServer) {
        ProxyHost proxyHost = null;
        if (useProxyServer != null) {
            String proxyHostStr = useProxyServer;
            int colonIdx = proxyHostStr.indexOf(':');
            if (colonIdx != -1) {
                proxyHostStr = proxyHostStr.substring(0, colonIdx);
                String proxyPortStr = useProxyServer.substring(colonIdx + 1);
                if (proxyPortStr != null && proxyPortStr.length() > 0 && proxyPortStr.matches("[0-9]+")) {
                    int proxyPort = Integer.parseInt(proxyPortStr);
                    proxyHost = new ProxyHost(proxyHostStr, proxyPort);
                } else {
                    proxyHost = new ProxyHost(proxyHostStr);
                }
            } else {
                proxyHost = new ProxyHost(proxyHostStr);
            }
        }
        return proxyHost;
    }

    private static HttpMethod setupProxyRequest(final HttpServletRequest hsRequest, final URL targetUrl) throws IOException {
        final String methodName = hsRequest.getMethod();
        final HttpMethod method;
        if ("POST".equalsIgnoreCase(methodName)) {
            PostMethod postMethod = new PostMethod();
            InputStreamRequestEntity inputStreamRequestEntity = new InputStreamRequestEntity(hsRequest.getInputStream());
            postMethod.setRequestEntity(inputStreamRequestEntity);
            method = postMethod;
        } else if ("GET".equalsIgnoreCase(methodName)) {
            method = new GetMethod();
        } else {
            log.warn("Unsupported HTTP method requested: " + hsRequest.getMethod());
            return null;
        }

        method.setFollowRedirects(false);
        method.setPath(targetUrl.getPath());
        method.setQueryString(targetUrl.getQuery());

        Enumeration e = hsRequest.getHeaderNames();
        if (e != null) {
            while (e.hasMoreElements()) {
                String headerName = (String) e.nextElement();
                if ("host".equalsIgnoreCase(headerName)) {
                    //the host value is set by the http client
                    continue;
                } else if ("content-length".equalsIgnoreCase(headerName)) {
                    //the content-length is managed by the http client
                    continue;
                } else if ("accept-encoding".equalsIgnoreCase(headerName)) {
                    //the accepted encoding should only be those accepted by the http client.
                    //The response stream should (afaik) be deflated. If our http client does not support
                    //gzip then the response can not be unzipped and is delivered wrong.
                    continue;
                }
/* Hippo customization: allow passing along cookies
                else if (headerName.toLowerCase().startsWith("cookie")) {
                    //fixme : don't set any cookies in the proxied request, this needs a cleaner solution
                    continue;
                }
*/

                Enumeration values = hsRequest.getHeaders(headerName);
                while (values.hasMoreElements()) {
                    String headerValue = (String) values.nextElement();
                    log.info("setting proxy request parameter:" + headerName + ", value: " + headerValue);
                    method.addRequestHeader(headerName, headerValue);
                }
            }
        }

        log.info("proxy query string " + method.getQueryString());
        return method;
    }

    private static void setupResponseHeaders(HttpMethod httpMethod, HttpServletResponse hsResponse) {
        if ( log.isInfoEnabled() ) {
            log.info("setupResponseHeaders");
            log.info("status text: " + httpMethod.getStatusText());
            log.info("status line: " + httpMethod.getStatusLine());
        }

        //filter the headers, which are copied from the proxy response. The http lib handles those itself.
        //Filtered out: the content encoding, the content length and cookies
        for (int i = 0; i < httpMethod.getResponseHeaders().length; i++) {
            Header h = httpMethod.getResponseHeaders()[i];
            if ("content-encoding".equalsIgnoreCase(h.getName())) {
                continue;
            } else if ("content-length".equalsIgnoreCase(h.getName())) {
                continue;
            } else if ("transfer-encoding".equalsIgnoreCase(h.getName())) {
                continue;
            }
/* Hippo customization: allow passing along cookies
            else if (h.getName().toLowerCase().startsWith("cookie")) {
                //retrieving a cookie which sets the session id will change the calling session: bad! So we skip this header.
                continue;
            } else if (h.getName().toLowerCase().startsWith("set-cookie")) {
                //retrieving a cookie which sets the session id will change the calling session: bad! So we skip this header.
                continue;
            }
*/

            hsResponse.addHeader(h.getName(), h.getValue());
            if ( log.isInfoEnabled() ) log.info("setting response parameter:" + h.getName() + ", value: " + h.getValue());
        }
        //fixme what about the response footers? (httpMethod.getResponseFooters())

        if (httpMethod.getStatusCode() != 200) {
            hsResponse.setStatus(httpMethod.getStatusCode());
        }
    }

}


/**
 * @author Gunnar Hillert
 */
class RequestProxyCustomRequestEntity  implements RequestEntity {

    private InputStream is = null;
    private long contentLength = 0;
    private String contentType;

    public RequestProxyCustomRequestEntity(InputStream is, long contentLength, String contentType) {
        super();
        this.is = is;
        this.contentLength = contentLength;
        this.contentType = contentType;
    }

    public boolean isRepeatable() {
        return true;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void writeRequest(OutputStream out) throws IOException {

        try {
            int l;
            byte[] buffer = new byte[10240];
            while ((l = is.read(buffer)) != -1) {
                out.write(buffer, 0, l);
            }
        } finally {
            is.close();
        }
    }

    public long getContentLength() {
        return this.contentLength;
    }
}
