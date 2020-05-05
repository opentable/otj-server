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
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.lang.NonNull;
//TODO: make sure dmitry tested with Cloud Config - concerned about order of precedence
public class SpringPortSelectionPostProcessor implements EnvironmentPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SpringPortSelectionPostProcessor.class);
    private static final String JMX_PROPERTY_SOURCE = "ot-jmx-properties";
    public static final String PROCESSOR_TEST= "environment-processor-test";

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        Arrays.stream(environment.getActiveProfiles())
                .filter(t -> ("deployed".equals(t) || PROCESSOR_TEST.equals(t)))
                .filter(i -> "true".equalsIgnoreCase(environment.getProperty("ot.port-selector.enabled", "true")))
                .findFirst()
                .map(i -> environment)
                .ifPresent(this::injectPortSelectorPropertySource);
    }

    private void injectPortSelectorPropertySource(ConfigurableEnvironment environment) {
        environment.getPropertySources()
                .remove(OtPortSelectorPropertySource.class.getName());
        environment.getPropertySources()
                .remove(OtPortSelectorInfoPropertySource.class.getName());
        environment.getPropertySources()
                .remove(JMX_PROPERTY_SOURCE);

        final PortSelector portSelector = new PortSelector(environment);
        environment.getPropertySources()
                .addFirst(getPortPropertySource(portSelector));
        environment.getPropertySources()
                .addLast(getPortDebugPropertySource(portSelector));
        environment.getPropertySources()
                .addFirst(getHostPropertySource(environment));
    }

    private MapPropertySource getHostPropertySource(ConfigurableEnvironment environment) {
        /*
         * fix jmx host names for Kubernetes
         */
        final Map<String, Object> map = new HashMap<>();
        boolean isKubernetes = PortSelector.isKubernetes(environment);
        if (isKubernetes) {
            map.put(PortSelector.JMX_ADDRESS, "127.0.0.1"); //NOPMD
            LOG.info("Assigned 127.0.0.1 to JMX address, since it's on kubernetes");
            // No point, since this must be a -D system property as java starts up
            if (environment.containsProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME)) {
                if (!"127.0.0.1".equals(environment.getProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME))) { //NOPMD
                    LOG.warn("JMX provided but {} should be 127.0.0.1", JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME); //NOPMD
                }
            }
        }
        return new MapPropertySource(JMX_PROPERTY_SOURCE, map);
    }
    private PropertySource<Integer> getPortPropertySource(PortSelector portSelector) {
        final Map<String, PortSelector.PortSelection> portSelectionMap = portSelector.getPortSelectionMap();
        final StringBuilder sb = new StringBuilder(4096);
        portSelectionMap.forEach((key, value) -> sb.append(key).append(" ==> ").append(value.toString()).append('\r'));
        LOG.info("\nPort Selections: \n{}", sb.toString());
        return new OtPortSelectorPropertySource(ImmutableMap.copyOf(portSelectionMap));
    }

    private PropertySource<String> getPortDebugPropertySource(PortSelector portSelector) {
        return new OtPortSelectorInfoPropertySource(ImmutableMap.copyOf(portSelector.getPortSelectionMap()));
    }

    public static class OtPortSelectorPropertySource extends PropertySource<Integer> {

        private final Map<String, PortSelector.PortSelection> portSelectionMap;

        public OtPortSelectorPropertySource(Map<String, PortSelector.PortSelection> portSelectionMap) {
            super(OtPortSelectorPropertySource.class.getName());
            this.portSelectionMap = portSelectionMap;
        }

        public Map<String, PortSelector.PortSelection> getPortSelectionMap() {
            return portSelectionMap;
        }

        @Override
        public Integer getProperty(@NonNull String propertyName) {
            if (portSelectionMap.containsKey(propertyName)) {
                return portSelectionMap.get(propertyName).getAsInteger();
            }
            return null;
        }
    }

    public static class OtPortSelectorInfoPropertySource extends EnumerablePropertySource<String> {

        private final Map<String, PortSelector.PortSelection> portSelectionMap;

        public OtPortSelectorInfoPropertySource(Map<String, PortSelector.PortSelection> portSelectionMap) {
            super(OtPortSelectorInfoPropertySource.class.getName());
            this.portSelectionMap = portSelectionMap;
        }

        @Override
        public String[] getPropertyNames() {
            return portSelectionMap.keySet()
                    .stream()
                    .map(s -> "ot.port-selector.info." + s)
                    .toArray(String[]::new);
        }

        @Override
        public Object getProperty(@NonNull String propertyName) {
            if (propertyName.startsWith("ot.port-selector.info.")) {
                propertyName = propertyName.replace("ot.port-selector.info.", "");
                if (portSelectionMap.containsKey(propertyName)) {
                    return portSelectionMap.get(propertyName).toString();
                }
            }
            return null;
        }
    }
}
