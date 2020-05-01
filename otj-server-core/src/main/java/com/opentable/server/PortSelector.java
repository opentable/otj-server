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

import static com.opentable.server.EmbeddedJettyBase.BOOT_CONNECTOR_NAME;
import static com.opentable.server.EmbeddedJettyBase.DEFAULT_CONNECTOR_NAME;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

import com.opentable.service.OnKubernetesCondition;

public class PortSelector {

    static final String MANAGEMENT_SERVER_PORT = "management.server.port";
    static final String JMX_ADDRESS = "ot.jmx.address";
    static final String JMX_PORT = "ot.jmx.port";
    static final String HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT = "ot.httpserver.connector.default-http.port";
    static final String SERVER_PORT = "server.port";
    private static final String SERVER_SSL_ENABLED = "server.ssl.enabled";

    public enum PortSource {
        FROM_SPRING_PROPERTY,
        FROM_PORT_ORDINAL,
        FROM_PORT_NAMED,
        FROM_DEFAULT_VALUE,
        NOT_FOUND
    }

    private final Environment environment;
    private final AtomicInteger portIndex = new AtomicInteger(0);

    public static class PortSelection {
        private final String payload;
        private final PortSource portSource;
        private final String sourceInfo;
        private final String originalPropertyName;

        public PortSelection(final String originalPropertyName, final String payload, final PortSource portSource, final String sourceInfo) {
            this.originalPropertyName = originalPropertyName;
            this.payload = payload;
            this.portSource = portSource;
            this.sourceInfo = sourceInfo;
        }

        public static PortSelection empty(String name) {
            return new PortSelection(name, null, PortSource.NOT_FOUND, null);
        }

        public String getSourceInfo() {
            return sourceInfo;
        }

        public PortSource getPortSource() {
            return portSource;
        }

        public String getPayload() {
            return payload;
        }

        public Integer getAsInteger() {
            return hasValue() ? Integer.parseInt(payload) : null;
        }

        public Long getAsLong() {
            return hasValue() ? Long.parseLong(payload) : null;
        }

        public boolean hasValue() {
            return StringUtils.isNotBlank(payload);
        }

        @Override
        public String toString() {
            return "PayloadResult{" + "payload='" + payload + '\'' +
                    ", payloadSource=" + portSource +
                    ", sourceInfo=" + sourceInfo +
                    '}';
        }
    }

    public PortSelector(Environment environment) {
        this.environment = environment;
    }

    private PortSelection get(String springPropertyName, String namedPort) {
        PortSelection portSelection;
        if (isKubernetes(environment)) {
            // For k8s, always prefer named ports, even override user supplied property.
            portSelection = get(springPropertyName, namedPort, PortSource.FROM_PORT_NAMED);
            if (!portSelection.hasValue()) {
                portSelection = get(springPropertyName, springPropertyName, PortSource.FROM_SPRING_PROPERTY);
            }
        } else {
            // for singularity, if property not set or set to -1, allocate PORTn
            portSelection = get(springPropertyName, springPropertyName, PortSource.FROM_SPRING_PROPERTY);
            if (!portSelection.hasValue() || "-1".equals(portSelection.getPayload())) {
                portSelection = get(springPropertyName, "PORT" + portIndex.getAndIncrement(), PortSource.FROM_PORT_ORDINAL);
            }
        }
       return portSelection;
    }

    public static boolean isKubernetes(Environment environment) {
        return "true".equalsIgnoreCase(environment.getProperty(OnKubernetesCondition.ON_KUBERNETES, "false"));
    }

    private PortSelection get(String name, String propertyName, PortSource portSource) {
        if (propertyName != null && environment.containsProperty(propertyName)) {
            final String propertyValue = environment.getProperty(propertyName);
            if (propertyValue != null) {
                return new PortSelection(name, propertyValue, portSource, propertyName);
            }
        }
        return PortSelection.empty(name);
    }

    public PortSelection getWithDefault(final String springProperty, final String namedPort, int defaultV) {
        final PortSelection portSelection = get(springProperty, namedPort);
        return portSelection.hasValue() ? portSelection : new PortSelection(springProperty, String.valueOf(defaultV), PortSource.FROM_DEFAULT_VALUE, String.valueOf(defaultV));
    }

    private PortSelection getJMXPort() {
        return getWithDefault(JMX_PORT, "PORT_JMX", 0);
    }

    private PortSelection getActuatorPort() {
        return get(MANAGEMENT_SERVER_PORT, "PORT_ACTUATOR");
    }

    public Map<String, PortSelection> getPortSelectionMap() {
        Map<String, PortSelection> res = Arrays.stream(environment.getProperty("ot.httpserver.active-connectors", "default-http").split(","))
                .map(String::trim)
                .map(connectorName -> {
                    if (connectorName.equals(BOOT_CONNECTOR_NAME)) {
                        final boolean sslEnabled = Boolean.parseBoolean(environment.getProperty(SERVER_SSL_ENABLED, "true"))
                                && environment.getProperty("server.ssl.key-store") != null;
                        return getWithDefault(SERVER_PORT, sslEnabled ? "PORT_HTTPS" : "PORT_HTTP", 8080);
                    }
                    if (connectorName.equals(DEFAULT_CONNECTOR_NAME)) {
                        final boolean sslEnabled = "https".equalsIgnoreCase(environment.getProperty("ot.httpserver.connector." + connectorName + ".protocol", "http"));
                        return getWithDefault("ot.httpserver.connector." + connectorName + ".port", sslEnabled ? "PORT_HTTPS" : "PORT_HTTP", 0);
                    }
                    return getWithDefault("ot.httpserver.connector." + connectorName + ".port", "PORT_" + connectorName.toUpperCase(Locale.US), 0);
                }).collect(Collectors.toMap(i -> i.originalPropertyName, Function.identity()));
        res.put(JMX_PORT, getJMXPort());
        res.put(MANAGEMENT_SERVER_PORT, getActuatorPort());
        return res;
    }
}
