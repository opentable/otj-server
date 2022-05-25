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
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.ProcessorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opentable.logging.CommonLogHolder;
import com.opentable.service.K8sInfo;

public class PreFlight {
    private static final String COMMIT = "X-BasePOM-Git-Commit-Id";

    private static final Logger LOG = LoggerFactory.getLogger(PreFlight.class);

    @Inject
    public PreFlight(K8sInfo k8sInfo) {
        LOG.debug("Setting k8sInfo kubernetes: {}, cluster: {}, namespace:{}",
                k8sInfo.isKubernetes(), k8sInfo.getClusterName().orElse(null), k8sInfo.getNamespace().orElse(null));
        CommonLogHolder.setK8sInfo(k8sInfo);
    }

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
            final Attributes attributes = mf.getMainAttributes();
            final StringBuilder sb = new StringBuilder(4096);
            final String implementationTitle = attributes.getValue(Attributes.Name.IMPLEMENTATION_TITLE);
            final String implementationVersion = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            final String commit = attributes.getValue(COMMIT);
            if (StringUtils.isNotBlank(implementationTitle)) {
                sb.append("Library: ").append(implementationTitle);
                if (StringUtils.isNotBlank(implementationVersion)) {
                    sb.append(" -- version: ").append(implementationVersion);
                }
                if (StringUtils.isNotBlank(commit)) {
                    sb.append(" -- commit: ").append(commit);
                }
                sb.append('\n');
            }
            LOG.debug(sb.toString());
        }
    }

    @PostConstruct
    public void start() {
        try {
            LOG.info("At startup: JVM {} processors, and Jetty {} processors", Runtime.getRuntime().availableProcessors(), ProcessorUtils.availableProcessors());
            readManifests();
        } catch (IOException e) {
            LOG.debug("Error while reading manifest", e);
        }
    }
}
