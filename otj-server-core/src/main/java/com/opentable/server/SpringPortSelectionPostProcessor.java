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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.NonNull;

import com.opentable.service.OnKubernetesCondition;
//TODO: make sure dmitry tested with Cloud Config - concerned about order of precedence
public class SpringPortSelectionPostProcessor implements EnvironmentPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SpringPortSelectionPostProcessor.class);
    private static final String JMX_PROPERTY_SOURCE = "ot-jmx-properties";
    public static final String PROCESSOR_TEST= "environment-processor-test";

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        Arrays.stream(environment.getActiveProfiles())
                .filter(t -> (t.equals("deployed") || t.equals(PROCESSOR_TEST)))
                .filter(i -> "true".equalsIgnoreCase(environment.getProperty("ot.port-selector.enabled", "true")))
                .findFirst()
                .map(i -> environment)
                .ifPresent(this::injectPortSelectorPropertySource);
    }

    private void injectPortSelectorPropertySource(ConfigurableEnvironment environment) {
        environment.getPropertySources()
                .remove(OtPortSelectorPropertySource.class.getName());
        environment.getPropertySources()
                .remove(JMX_PROPERTY_SOURCE);

        final PortSelector portSelector = new PortSelector(environment);
        environment.getPropertySources()
                .addLast(getPortPropertySource(environment, portSelector));
        environment.getPropertySources()
                .addLast(getHostPropertySource(environment));
    }

    private MapPropertySource getHostPropertySource(ConfigurableEnvironment environment) {
        /*
         * fix jmx host names for Kubernetes
         */
        final Map<String, Object> map = new HashMap<>();
        boolean isKubernetes = PortSelector.isKubernetes(environment);
        if (isKubernetes) {
            map.put(PortSelector.JMX_ADDRESS, "127.0.0.1"); //NOPMD
            // No point, since this must be a -D system proeprty as java starts up
            if (environment.containsProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME)) {
                if (!"127.0.0.1".equals(environment.getProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME))) { //NOPMD
                    LOG.warn("JMX provided but {} should be 127.0.0.1", JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME); //NOPMD
                }
            }
        }
        return new MapPropertySource(JMX_PROPERTY_SOURCE, map);
    }
    private PropertySource<Integer> getPortPropertySource(ConfigurableEnvironment environment, PortSelector portSelector) {
        final Map<String, PortSelector.PortSelection> portSelectionMap = portSelector.getPortSelectionMap();
        final StringBuilder sb = new StringBuilder(4096);
        portSelectionMap.forEach((key, value) -> sb.append(key).append(" ==> ").append(value.toString()).append('\r'));
        LOG.info("\nPort Selections: \n{}", sb.toString());
        return new OtPortSelectorPropertySource(environment, ImmutableMap.copyOf(portSelectionMap));
    }

    public static class OtPortSelectorPropertySource extends PropertySource<Integer> {
        private final ConfigurableEnvironment environment;
        private final Map<String, PortSelector.PortSelection> portSelectionMap;
        public OtPortSelectorPropertySource(ConfigurableEnvironment environment, Map<String, PortSelector.PortSelection> portSelectionMap) {
            super(OtPortSelectorPropertySource.class.getName());
            this.environment = environment;
            this.portSelectionMap = portSelectionMap;
        }

        public Map<String, PortSelector.PortSelection> getPortSelectionMap() {
            return portSelectionMap;
        }

        @Override
        public Integer getProperty(@NonNull String propertyName) {
            // First, short circuit and return nothing here.
            // These prevent infinite loops
            if (OnKubernetesCondition.ON_KUBERNETES.equalsIgnoreCase(propertyName)
                    || "ot.httpserver.connector.default.port".equalsIgnoreCase(propertyName)) {
                return null;
            }
            final boolean isK8s = PortSelector.isKubernetes(environment);

            //TODO: How strongly do you feel about case insensitivity dmitry?
            // Otherwise most of this could collapse down to
//            if (portSelectionMap.containsKey(propertyName)) {
//                return portSelectionMap.get(propertyName).getAsInteger();
//            }
            // Default spring boot
            if (PortSelector.SERVER_PORT.equalsIgnoreCase(propertyName)) {
                return portSelectionMap.get(PortSelector.SERVER_PORT).getAsInteger();
            }
            // otj-server default connector
            if (PortSelector.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT.equalsIgnoreCase(propertyName)) {
                return portSelectionMap.get(PortSelector.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT).getAsInteger();
            }

            // jmx
            if (PortSelector.JMX_PORT.equalsIgnoreCase(propertyName)) {
                return portSelectionMap.get(PortSelector.JMX_PORT).getAsInteger();
            }
            // actuator
            if (PortSelector.MANAGEMENT_SERVER_PORT.equalsIgnoreCase(propertyName)) {
                return portSelectionMap.get(PortSelector.MANAGEMENT_SERVER_PORT).getAsInteger();
            }

            // otj-server named connector
            // This is safe, because it won't be queried if it's not in the list, otherwise
            // it would insert a value that shouldn't exist
            //TODO: Discuss with Lu - this would ignore the CURRENT property. I think it should
            // return null instead?
            //TODO: Dmitry since not all PropertySources are enumerable, I don't think this can be calculated statically, sigh
            if (isK8s && propertyName.matches("ot\\.httpserver\\.connector\\..*\\.port")) {
                final String namedPort = propertyName.split("\\.")[3].toUpperCase(Locale.US);
                return  Integer.parseInt(environment.getProperty("PORT_" + namedPort, "-1"));
            }

            return null;
        }
    }
}
