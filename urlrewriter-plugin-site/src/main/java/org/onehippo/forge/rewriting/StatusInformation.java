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

package org.onehippo.forge.rewriting;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;

import org.hippoecm.hst.site.HstServices;
import org.onehippo.forge.rewriting.repo.RewritingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.Status;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

public class StatusInformation extends Status {

    private static final Logger log = LoggerFactory.getLogger(StatusInformation.class);
    private static final String VERSION_UNKNOWN = "UNKNOWN";

    public StatusInformation(final Conf conf, final UrlRewriteFilter urlRewriteFilter) {
        super(conf, urlRewriteFilter);
    }

    @Override
    public void displayStatusInContainer(final HttpServletRequest hsRequest) {
        displayHippoInformation();
        super.displayStatusInContainer(hsRequest);
    }

    private void displayHippoInformation() {
        final String version = extractVersion();
        final Map<String, Object> properties = getConfiguration();

        getBuffer().append("<div style=\"background-color: #cdf2ff; font-family: Arial, sans-serif; padding: 2px 15px 0px;\">")
                   .append("  <h1 style=\"font-size: 120%\">Hippo URL Rewriter</h1>")
                   .append("  <pre>Version: ").append(version).append("</pre>\n")
                   .append("  <pre>\n");
        for (String s : properties.keySet()) {
            getBuffer().append(s)
                       .append("=")
                       .append(properties.get(s))
                       .append("\n");
        }

        getBuffer().append("  </pre>\n")
                   .append("</div>\n");
    }

    private String extractVersion() {
        final Class clazz = getClass();
        final String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            return VERSION_UNKNOWN;
        }
        final int idx = classPath.lastIndexOf('!');
        if (idx < 0) {
            return VERSION_UNKNOWN;
        }
        final int endIndex = idx + 1;
        if (classPath.length() < endIndex) {
            return VERSION_UNKNOWN;
        }
        String manifestPath = classPath.substring(0, endIndex) + "/META-INF/MANIFEST.MF";
        final URL url;
        try {
            url = new URL(manifestPath);
        } catch (MalformedURLException ignore) {
            return VERSION_UNKNOWN;
        }

        try (InputStream stream = url.openStream()) {
            final Manifest manifest = new Manifest(stream);
            final Attributes attr = manifest.getMainAttributes();
            final String value = attr.getValue("Implementation-Version");
            if (Strings.isNullOrEmpty(value)) {
                return VERSION_UNKNOWN;
            }
            return value;

        } catch (java.io.FileNotFoundException ignore) {
        } catch (IOException | IllegalArgumentException e) {
            log.error("Error reading manifest stream", e);
        }

        return VERSION_UNKNOWN;
    }

    private Map<String,Object> getConfiguration() {
        final RewritingManager rewritingManager =
                HstServices.getComponentManager().getComponent(RewritingManager.class.getName());

        final Map<String, Object> result = new HashMap<>();
        result.put("skipPost", rewritingManager.getSkipPOST());
        result.put("skippedPrefixes", Arrays.toString(rewritingManager.getSkippedPrefixes()));
        result.put("useQueryString", rewritingManager.isUseQueryString());
        result.put("ignoreContextPath", rewritingManager.getIgnoreContextPath());

        return result;
    }
}

