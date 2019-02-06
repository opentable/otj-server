/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;

import org.eclipse.jetty.util.ProcessorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreFlight {
    private static final String COMMIT = "X-BasePOM-Git-Commit-Id";

    private static final Logger LOG = LoggerFactory.getLogger(PreFlight.class);

    public void readManifests() throws IOException {
        final Enumeration<URL> urlsEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
        while (urlsEnum.hasMoreElements()) {
            readManifest(urlsEnum.nextElement());
        }
    }

    private void readManifest(final URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            final Manifest mf = new Manifest();
            mf.read(is);
            final Attributes atts = mf.getMainAttributes();
            LOG.debug("Starting up: {} version {} - built from commit {}",
                    atts.getValue(Attributes.Name.IMPLEMENTATION_TITLE),
                    atts.getValue(Attributes.Name.IMPLEMENTATION_VERSION),
                    atts.getValue(COMMIT)
            );
        }
    }

    @PostConstruct
    public void start() {
        try {
            LOG.info("Starting up, JVM {} processors, and Jetty {} processors", Runtime.getRuntime().availableProcessors(), ProcessorUtils.availableProcessors());
            readManifests();
        } catch (IOException e) {
            LOG.debug("Error while reading manifest", e);
        }
    }
}
