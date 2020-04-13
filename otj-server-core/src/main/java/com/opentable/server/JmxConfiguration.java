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
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import com.google.common.base.MoreObjects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.stereotype.Component;

import com.opentable.server.JmxConfiguration.JmxmpServer;
import com.opentable.service.AppInfo;
import com.opentable.service.K8sInfo;
import com.opentable.service.PayloadSelector;

/**
 * JMX Configuration.
 *
 * <p>
 * Note that this configuration class injects a static {@link MBeanServer}. This will fail if you are creating
 * multiple contexts in the same process and attempting to register MBeans. When might you do this? A natural
 * circumstance is integration testing, in which you might spin up two servers in the same process and have
 * them talk to one another. Or, as another example, you might spin up a Discovery server and then wire up
 * two other servers to discover each other, all in the same process. To handle this circumstance
 * appropriately, consult the {@code TestMBeanServerConfiguration}, in the testing package of this codebase.
 */
@Configuration
@Import(JmxmpServer.class)
public class JmxConfiguration {

    @Bean
    public AnnotationMBeanExporter mbeanExporter(MBeanServer mBeanServer) {
        AnnotationMBeanExporter exporter = new AnnotationMBeanExporter();
        exporter.setServer(mBeanServer);
        return exporter;
    }

    private static final String LEGACY_JMX = "service:jmx:jmxmp://%s:%s";

    @Bean
    public MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    @Component
    static class JmxmpServer {
        private static final String JAVA_RMI_SERVER_HOSTNAME = "java.rmi.server.hostname";
        private static final Logger LOG = LoggerFactory.getLogger(JmxmpServer.class);
        static final String WILDCARD_BIND = "0.0.0.0"; // NOPMD

        @Value("${ot.jmx.address:#{null}}")
        private String jmxAddress;

        @Value("${ot.jmx.url-format:service:jmx:jmxmp://%s:%s}")
        private String urlFormat;

        @Value("${ot.jmx.enabled:#{true}}")
        private boolean jmxEnabled;

        private final MBeanServer mbs;
        private final K8sInfo k8sInfo;
        private final AppInfo appInfo;
        private final PayloadSelector payloadSelector;

        private JMXConnectorServer server;


        @Inject
        JmxmpServer(K8sInfo k8sInfo, AppInfo app, PayloadSelector payloadSelector,  MBeanServer mbs) {
            this.mbs = mbs;
            this.k8sInfo = k8sInfo;
            this.appInfo = app;
            this.payloadSelector = payloadSelector;
        }

        @PostConstruct
        public void start() throws IOException {
            // Always need this - effectively it's a noop since old code references PORT1 directly.
            PayloadSelector.PayloadResult payloadResult = payloadSelector.getJMXPort();
            OptionalInt jmxPort = payloadResult.getAsInteger();
            if (!jmxPort.isPresent() || jmxPort.getAsInt() <= 0) {
                LOG.info("No JMX port set, not exporting. JMX configuration disabled");
                return;
            } else {
                LOG.info("jmxPort {}", payloadResult);
            }

            if (k8sInfo.isKubernetes()) {
                LOG.info("In kubernetes, force the jmxAddress to be 127.0.0.1 instead of {}", jmxAddress);
                jmxAddress = "127.0.0.1"; //NOPMD
            }

            if (LEGACY_JMX.equals(urlFormat)) {
                // Honestly we'd prefer it's always false and not here at all. But for compatibility (consider newer coommon config, older pom and hence otj-server)...
                // This lets us eventually turn it off once we decide all otj-server's we care about have this switch
                final String bind = MoreObjects.firstNonNull(jmxAddress, WILDCARD_BIND);
                if (!jmxEnabled) {
                    LOG.info("Programmatic JMX Configuration is disabled. You must have command line options set, or JMX won't be accessible.");
                    if (System.getProperty(JAVA_RMI_SERVER_HOSTNAME) == null) {
                        LOG.debug("Looks like it's not set up -- ");
                        logCommandLineOptions(k8sInfo.isKubernetes(), "$TASK_HOST");
                    }
                    return;
                }

                final String simpleURL = String.format(
                        urlFormat,
                        bind,
                        jmxPort.getAsInt());
                LOG.info("Starting jmx with jmxmp support bound to {} You'll need to connnect to this service using the jmxmp jar and protocol, using \n\t{} " +
                        "\n\tSee https://wiki.otcorp.opentable.com/x/YsoIAQ for more information.", bind, simpleURL);
                LOG.debug("Alternatively, switch ot.jmx.enabled=false in application-deployed.properties and set jvm.properties options (Recommended). Then you can just connect via {}:{}",
                        appInfo.getTaskHost(), jmxPort.getAsInt());
                logCommandLineOptions(k8sInfo.isKubernetes(), "$TASK_HOST");
                final String url = String.format(
                        urlFormat,
                        bind,
                        jmxPort.getAsInt());
                server = doLegacy(url);
                server.start();
            } else {
                throw new UnsupportedOperationException("We only support jmxmp currently");
            }


            /*
             * Keeping for future generations. The following works and implements things as desired without use of jmxmp.
             * Why don't we use it? Because it uses com.sun internal implementation, and I was unable to port it.
             *
             * https://opentable.atlassian.net/browse/OTPL-2702 was the motivation for this failed but informative
             * venture.
             *             Properties properties = new Properties();
             *             properties.put("com.sun.management.jmxremote", "true");
             *             properties.put("com.sun.management.jmxremote.port", String.valueOf(jmxPort));
             *             properties.put("com.sun.management.jmxremote.rmi.port", String.valueOf(jmxPort));
             *             properties.put("com.sun.management.jmxremote.ssl", "false");
             *             properties.put("com.sun.management.jmxremote.authenticate", "false");
             *             properties.put("com.sun.management.hostname.local.only", "false");
             *             properties.put(JAVA_RMI_SERVER_HOSTNAME, jmxHost);
             *             ConnectorBootstrap.initialize(String.valueOf(jmxPort), properties);
             */

        }

        private void logCommandLineOptions(boolean isKubernetes, String bind) {
            String portName = isKubernetes ? "PORT_JMX" : "PORT1";
            String bindName = isKubernetes ? "127.0.0.1" : bind; //NOPMD
            LOG.debug("In jvm.propertes, add" +
            "\n\t-Dcom.sun.management.jmxremote=true" +
            "\n\t-Dcom.sun.management.jmxremote.port=$" + portName +
            "\n\t-Dcom.sun.management.jmxremote.rmi.port=$" + portName +
            "\n\t-Dcom.sun.management.jmxremote.ssl=false" +
            "\n\t-Dcom.sun.management.jmxremote.authenticate=false" +
            "\n\t-Dcom.sun.management.jmxremote.local.only=false" +
            "\n\t-Djava.rmi.server.hostname=" + bindName);
        }

        private JMXConnectorServer doLegacy(String url) throws IOException {
            LOG.info("Starting JMX Connector Server '{}'", url);

            Map<String, String> jmxEnv = new HashMap<>();
            jmxEnv.put("jmx.remote.server.address.wildcard",
                    Boolean.toString(jmxAddress == null));

            JMXServiceURL jmxUrl = new JMXServiceURL(url);
            return JMXConnectorServerFactory.newJMXConnectorServer(jmxUrl, jmxEnv, mbs);
        }

        @PreDestroy
        public void close() throws IOException {
            if (server != null) {
                server.stop();
                server = null;
            }
        }

    }
}
