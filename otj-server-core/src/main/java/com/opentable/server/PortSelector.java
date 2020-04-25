package com.opentable.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import com.opentable.service.OnKubernetesCondition;

public class PortSelector {

    private final Environment environment;
    static final String MANAGEMENT_SERVER_PORT = "management.server.port";
    static final String JMX_ADDRESS = "ot.jmx.address";
    static final String JMX_PORT = "ot.jmx.port";
    static final String HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT = "ot.httpserver.connector.default-http.port";
    private static final String HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PROTOCOL = "ot.httpserver.connector.default-http.protocol";
    static final String SERVER_PORT = "server.port";
    private static final String SERVER_SSL_ENABLED = "server.ssl.enabled";
    public enum PortSource {
        FROM_SPRING_PROPERTY,
        FROM_PORT_ORDINAL,
        FROM_PORT_NAMED,
        FROM_DEFAULT_VALUE,
        NOT_FOUND
    }

    public static class PortSelection {
        private final String payload;
        private final PortSource portSource;
        private final String sourceInfo;

        public PortSelection(final String payload, final PortSource portSource, final String sourceInfo) {
            this.payload = payload;
            this.portSource = portSource;
            this.sourceInfo = sourceInfo;
        }

        public static PortSelection empty() {
            return new PortSelection(null, PortSource.NOT_FOUND, null);
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
            return  "PayloadResult{" + "payload='" + payload + '\'' +
                    ", payloadSource=" + portSource +
                    ", sourceInfo=" + sourceInfo +
                    '}';
        }
    }

    public PortSelector(Environment environment) {
        this.environment = environment;
    }

    private PortSelection get(
                                      String springPropertyName,
                                      int ordinal,
                                      String portName) {
        // First the named port - hence on k8s this gets precedence
        PortSelection portSelection = get(portName, PortSource.FROM_PORT_NAMED);
        if (!portSelection.hasValue()) {
            // Then the "official" spring property
            portSelection =  get(springPropertyName, PortSource.FROM_SPRING_PROPERTY);
            if (!portSelection.hasValue()) {
                // Finally the PORTn using a best guess here.
                portSelection = get("PORT" + ordinal, PortSource.FROM_PORT_ORDINAL);
            }
        }
        return portSelection;
    }

    public static boolean isKubernetes(ConfigurableEnvironment environment) {
        return "true".equalsIgnoreCase(environment.getProperty(OnKubernetesCondition.ON_KUBERNETES, "false"));
    }

    private PortSelection get(String propertyName, PortSource portSource) {
        if (propertyName != null && environment.containsProperty(propertyName))  {
            String propertyValue = environment.getProperty(propertyName);
            if (propertyValue != null) {
                return new PortSelection(propertyValue, portSource, propertyName);
            }
        }
        return PortSelection.empty();
    }

    public PortSelection getWithDefault(final String springProperty, final int ordinal, final String namedPort, Integer defaultV) {
        PortSelection portSelection = get(springProperty, ordinal, namedPort );
        return portSelection.hasValue() ? portSelection : new PortSelection(String.valueOf(defaultV), PortSource.FROM_DEFAULT_VALUE, String.valueOf(defaultV));
    }

    PortSelection getJMXPort() {
        return getWithDefault(
                JMX_PORT,
                 1,
                 "PORT_JMX",
                null);
    }

    PortSelection getActuatorPort() {
        return getWithDefault(MANAGEMENT_SERVER_PORT,
                2,
                "PORT_ACTUATOR",0);
    }

    public Map<String, PortSelection> getPortSelectionMap() {
        final Map<String, PortSelector.PortSelection> portSelectionMap = new HashMap<>();
        // server.port

        //TODO: won't this conflict with the default-http? Discuss with Dmitry.
        //TODO: Dmitry, the docs I found claimed ssl is on by default? and keystore is what in combo activates it
        String namedPort = Boolean.parseBoolean(
                environment.getProperty(SERVER_SSL_ENABLED, "true")) && environment.getProperty("server.ssl.key-store") != null ? "PORT_HTTPS" : "PORT_HTTP";
        //TODO: discuss with dmitry: shouldn't the default be null? eg not flipping this on at all
        portSelectionMap.put(SERVER_PORT, getWithDefault(SERVER_PORT, 0, namedPort, 8080 ));

        namedPort = "http".equalsIgnoreCase(
                environment.getProperty(HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PROTOCOL, "http")) ? "PORT_HTTP"
                : "PORT_HTTPS";
        // ot.httpserver.connector.default-http.port
        portSelectionMap.put(HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT,
                getWithDefault(HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT, 0, namedPort, -1));

        // jmx
        portSelectionMap.put(JMX_PORT, getJMXPort());
        // actuator
        //TODO: discuss with dmitry: shouldn't the default be null? eg not flipping this on at all
        portSelectionMap.put(MANAGEMENT_SERVER_PORT, getActuatorPort());
        return portSelectionMap;
    }
}
