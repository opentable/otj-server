package com.opentable.server;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

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
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import com.opentable.server.JmxConfiguration.JmxmpServer;

@Configuration
@Import(JmxmpServer.class)
@EnableMBeanExport
public class JmxConfiguration {
    @Bean
    public MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    @Component
    static class JmxmpServer {
        private static final Logger LOG = LoggerFactory.getLogger(JmxmpServer.class);
        static final String WILDCARD_BIND = "0.0.0.0"; // NOPMD

        @Value("${ot.jmx.port:${PORT1:#{null}}}")
        private Integer jmxPort;

        @Value("${ot.jmx.address:#{null}}")
        private String jmxAddress;

        @Value("${ot.jmx.url-format:service:jmx:jmxmp://%s:%s}")
        private String urlFormat;

        private final MBeanServer mbs;
        private JMXConnectorServer server;

        @Inject
        JmxmpServer(MBeanServer mbs) {
            this.mbs = mbs;
        }

        @PostConstruct
        public void start() throws IOException {
            if (jmxPort == null) {
                LOG.info("No JMX port set, not exporting");
                return;
            }

            final String url = String.format(
                    urlFormat,
                    MoreObjects.firstNonNull(jmxAddress, WILDCARD_BIND),
                    jmxPort);

            LOG.info("Starting JMX Connector Server '{}'", url);

            Map<String, String> jmxEnv = new HashMap<>();
            jmxEnv.put("jmx.remote.server.address.wildcard",
                    Boolean.toString(jmxAddress == null));

            JMXServiceURL jmxUrl = new JMXServiceURL(url);
            server = JMXConnectorServerFactory.newJMXConnectorServer(jmxUrl, jmxEnv, mbs);
            server.start();
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
