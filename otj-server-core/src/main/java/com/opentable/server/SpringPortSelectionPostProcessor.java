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
import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.lang.NonNull;

public class SpringPortSelectionPostProcessor implements EnvironmentPostProcessor {

    private static final String PREFIX = "ot.port-selector.defaults.";

    static final String MANAGEMENT_SERVER_PORT = "management.server.port";
    static final String JMX_PORT = "ot.jmx.port";
    static final String HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT = "ot.httpserver.connector.default-http.port";
    static final String SERVER_PORT = "server.port";

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        Arrays.stream(environment.getActiveProfiles())
                .filter("deployed"::equals)
                .filter(i -> "true".equalsIgnoreCase(environment.getProperty("ot.port-selector.enabled", "true")))
                .findFirst()
                .map(i -> environment)
                .ifPresent(this::injectPortSelectorPropertySource);
    }

    private void injectPortSelectorPropertySource(ConfigurableEnvironment environment) {
        environment.getPropertySources()
                .remove(OtPortSelectorPropertySource.class.getName());
        environment.getPropertySources()
                .addLast(new OtPortSelectorPropertySource(environment));
    }

    protected static class OtPortSelectorPropertySource extends EnumerablePropertySource<Integer> {

        private final ConfigurableEnvironment environment;

        public OtPortSelectorPropertySource(ConfigurableEnvironment environment) {
            super(OtPortSelectorPropertySource.class.getName());
            this.environment = environment;
        }

        @Override
        public @NonNull String[] getPropertyNames() {
            return new String[] {
                    PREFIX + SERVER_PORT,
                    PREFIX + HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT,
                    PREFIX + JMX_PORT,
                    PREFIX + MANAGEMENT_SERVER_PORT
            };
        }

        @Override
        public Integer getProperty(@NonNull String propertyName) {
            boolean isK8s = (!"IS_KUBERNETES".equalsIgnoreCase(propertyName)) && "true".equalsIgnoreCase(environment.getProperty("IS_KUBERNETES", "false"));
            final String s = propertyName.replace(PREFIX, "");
            // Default spring boot
            if (SERVER_PORT.equalsIgnoreCase(s)) {
                return Integer.parseInt(environment.getProperty("PORT_HTTP",
                        environment.getProperty("PORT0", "0")));
            }
            // otj-server default connector
            if (HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT.equalsIgnoreCase(s)) {
                return Integer.parseInt(environment.getProperty("PORT_HTTP",
                        environment.getProperty("PORT0", "-1")));
            }
            // otj-server named connector
            if (isK8s && s.matches("ot\\.httpserver\\.connector\\..*-.*\\.port")) {
                final String name = s.split("\\.")[3].toUpperCase(Locale.US);
                return  Integer.parseInt(environment.getProperty("PORT_" + name, "-1"));
            }
            // jmx
            if (JMX_PORT.equalsIgnoreCase(s)) {
                return Integer.parseInt(environment.getProperty("PORT_JMX",
                        environment.getProperty("PORT1", "0")));
            }
            // actuator
            if (MANAGEMENT_SERVER_PORT.equalsIgnoreCase(s)) {
                return Integer.parseInt(environment.getProperty("PORT_ACTUATOR",
                        environment.getProperty("PORT2", "0")));
            }
            return null;
        }
    }
}
