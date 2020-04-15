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
import org.springframework.core.env.PropertySource;

public class SpringPortSelectionPostProcessor implements EnvironmentPostProcessor {

    private static final String PORT_SELECTOR_PROPERTY_SOURCE = "OtPortSelectorPropertySource";
    static final String MANAGEMENT_SERVER_PORT = "management.server.port";
    static final String JMX_PORT = "ot.jmx.port";
    static final String HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT = "ot.httpserver.connector.default-http.port";
    static final String SERVER_PORT = "server.port";

    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        Arrays.stream(environment.getActiveProfiles())
                .filter("deployed"::equals)
                .findFirst()
                .map(i -> environment)
                .ifPresent(this::injectPortSelectorPropertySource);
    }

    private  void  injectPortSelectorPropertySource(ConfigurableEnvironment environment) {
        environment.getPropertySources()
                .remove(PORT_SELECTOR_PROPERTY_SOURCE);
        environment.getPropertySources()
                .addLast(new PropertySource<Integer>(PORT_SELECTOR_PROPERTY_SOURCE) {
                    @Override
                    public Integer getProperty(String s) {
                        // Default spring boot
                        if (SERVER_PORT.equalsIgnoreCase(s)) {
                            return Integer.parseInt(environment.getProperty("PORT_HTTP",
                                    environment.getProperty("PORT0", "0")));
                        }
                        // otj-server default connector
                        if (HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT.equalsIgnoreCase(s)) {
                            return Integer.parseInt(environment.getProperty("PORT_HTTP",
                                    environment.getProperty("PORT0", "0")));
                        }
                        // otj-server named connector
                        if (s.matches("ot\\.httpserver\\.connector\\..*\\.port")) {
                            final String name = s.split("\\.")[3].toUpperCase(Locale.US);
                            return Integer.parseInt(environment.getProperty("PORT_" + name,"0"));
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
                });
    }
}
