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
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.Conf;
import org.tuckey.web.filters.urlrewrite.Status;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

public class StatusInformation extends Status {


    private static final Logger log = LoggerFactory.getLogger(StatusInformation.class);
    private static final String VERSION_UNKNOWN = "UNKNOWN";

    public StatusInformation(final Conf conf) {
        super(conf);
    }

    public StatusInformation(final Conf conf, final UrlRewriteFilter urlRewriteFilter) {
        super(conf, urlRewriteFilter);
    }

    @Override
    public void displayStatusInContainer(final HttpServletRequest hsRequest) {
        super.displayStatusInContainer(hsRequest);
        addVersionNumber();
    }

    private void addVersionNumber() {
        final String version = extractVersion();
        getBuffer()
                .append("<h2>URL Rewriter Version</h2>")
                .append("<p>")
                .append(version)
                .append("</p>")
                .append('\n');
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
}

