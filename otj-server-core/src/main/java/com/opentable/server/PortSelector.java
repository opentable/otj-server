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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import com.opentable.service.OnKubernetesCondition;

public class PortSelector {
    private static final Logger LOG = LoggerFactory.getLogger( PortSelector.class );

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

    private final AtomicInteger portIndex = new AtomicInteger(0);
    private final int maximumPortIndex;

    public PortSelector(Environment environment) {
        this.environment = environment;
        int index = -1;
        while (true) {
            index++;
            if (!environment.containsProperty("PORT" + index)) {
                break;
            }
        }
        this.maximumPortIndex = index;
    }

    private synchronized int allocateNewPort(String springPropertyName) {
        final int nextPort = portIndex.getAndIncrement();
        final String ordinalPortName = "PORT" + nextPort;
        if (nextPort > maximumPortIndex) {
            LOG.warn("Trying to allocate {} for {}, but this port is not defined", ordinalPortName, springPropertyName);
        } else {
            LOG.debug("Allocated {} for {} ", ordinalPortName, springPropertyName);
        }
        // Regardless of conflict, just return it, though it will fail in some circumstances
        return nextPort;

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
            //TODO: I still don't like this magic -1, which is arguably only true for otj http -ping
            if (!portSelection.hasValue() || "-1".equals(portSelection.getPayload())) {
                final String ordinalPortName = "PORT" + allocateNewPort(springPropertyName);
                portSelection = get(springPropertyName, ordinalPortName, PortSource.FROM_PORT_ORDINAL);
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
                    /*
                     * Singularity:
                     *      Try spring property, then ordinal (up to allocated ports), then default value
                     * Kubernetes:
                     *      Try named port, then spring property, then default value
                     * Hence I see following possibilities
                     * - In Kubernetes, since PORT_HTTP/PORT_HTTPS may clash, boot + default http can have issues.
                     * We log and detect this and offer PORT_BOOT as a workaround.
                     *
                     * Named http connectors are similar. The obvious thing to do in Kubernetes, which I think is fine, is to
                     * have most people using named ports injected. That seems eminently reasonable.
                     * //TODO: discuss with lu if this seems reasonable - ping
                     *
                     * The other thing worth mulling is corner cases where falling back to ordinal loop is a problem. It shouldn't be
                     * of course, but it's worth thinking about. Can we catch and warn about this?
                     * More concretely, if they actually used an Ordinal Port in their spring property, can
                     * we log conflicts? That would require the constructor to loop through and get all ordinal values
                     * in a Map<Integer,Integer>. Is this worth it.
                     */
                    if (connectorName.equals(BOOT_CONNECTOR_NAME)) {
                        final boolean sslEnabled = Boolean.parseBoolean(environment.getProperty(SERVER_SSL_ENABLED, "true"))
                                && environment.getProperty("server.ssl.key-store") != null;
                        String namedPort = sslEnabled ? "PORT_HTTPS" : "PORT_HTTP";
                        if (environment.containsProperty("PORT_BOOT")) {
                            namedPort = "PORT_BOOT";
                        }
                        return getWithDefault(SERVER_PORT, namedPort, 8080);
                    }
                    if (connectorName.equals(DEFAULT_CONNECTOR_NAME)) {
                        final boolean sslEnabled = "https".equalsIgnoreCase(environment.getProperty("ot.httpserver.connector." + connectorName + ".protocol", "http"));
                        return getWithDefault("ot.httpserver.connector." + connectorName + ".port", sslEnabled ? "PORT_HTTPS" : "PORT_HTTP", 0);
                    }
                    return getWithDefault("ot.httpserver.connector." + connectorName + ".port", "PORT_" + connectorName.toUpperCase(Locale.US), 0);
                }).collect(Collectors.toMap(i -> i.originalPropertyName, Function.identity()));

        PortSelection serverPort = res.get(PortSelector.SERVER_PORT);
        PortSelection defaultHttpPort = res.get(PortSelector.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT);
        if (serverPort != null && defaultHttpPort != null &&
                serverPort.getSourceInfo() != null && serverPort.getSourceInfo().equals(defaultHttpPort.getSourceInfo())) {
            LOG.warn("You have defined both the spring boot connector and the default http connector. Under Kubernetes this doesn't function correctly\n.Define a variable PORT_BOOT to workaround this.");
        }

        /*
         * Singularity:
         * - Try spring property, then ordinal (up to allocated ports)
         * Kubernetes:
         * - Try named port, then spring property
         *
         * JMX
         *     - In Singularity, they've probably not defined the property (few people do), hence it will usually try to grab a spare port.
         * The default value is 0 if no ports available, which disables it. (See JMXConfiguration). That's fine. Note the "other"
         * JMX (via -D) will have some trouble with this rearrangement potentially, which maybe we should talk about.
         *     - In Kubernetes, the JMX port will normally have a named port if they follow instructions. If they don't, then that's
         *     problematic, but seems dealable.
         *
         * Actuator
         *      Note: This implementation skips the default value call, so it won't be set that way if they don't have a spare port.
         *
         *      - In Singularity they probably defined the property if they need it. Otherwise, the check below
         * will avoid allocating unavailable or unneeded ports in Singularity if actuator support has not been installed.
         *      - In Kubernetes they SHOULD have a named port, but otherwise it will try the spring property. Otherwise, it
         *      disables
         *
         */
        if (isKubernetes(environment) || "true".equals(environment.getProperty("ot.components.features.otj-actuator.enabled", "false"))) {
            res.put(MANAGEMENT_SERVER_PORT, getActuatorPort());
        }
        res.put(JMX_PORT, getJMXPort());
        return res;
    }
}
