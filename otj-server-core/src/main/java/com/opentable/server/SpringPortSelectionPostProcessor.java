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
                        if ("server.port".equalsIgnoreCase(s)) {
                            return Integer.parseInt(environment.getProperty("PORT_HTTP",
                                    environment.getProperty("PORT0", "0")));
                        }
                        // otj-server default connector
                        if ("ot.httpserver.connector.default-http.port".equalsIgnoreCase(s)) {
                            return Integer.parseInt(environment.getProperty("PORT_HTTP",
                                    environment.getProperty("PORT0", "0")));
                        }
                        // otj-server named connector
                        if (s.matches("ot\\.httpserver\\.connector\\..*\\.port")) {
                            final String name = s.split("\\.")[3].toUpperCase(Locale.US);
                            return Integer.parseInt(environment.getProperty("PORT_" + name,"0"));
                        }
                        // jmx
                        if ("ot.jmx.port".equalsIgnoreCase(s)) {
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
