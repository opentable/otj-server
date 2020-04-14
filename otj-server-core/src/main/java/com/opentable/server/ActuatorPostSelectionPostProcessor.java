package com.opentable.server;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import com.opentable.service.PortSelector;

public class ActuatorPostSelectionPostProcessor implements EnvironmentPostProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ActuatorPostSelectionPostProcessor.class);
    public static final String ACTUATOR_ENV_KEY = "management.server.port";
    @Override
    public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
        final PortSelector portSelector = new PortSelector(environment);
        final PortSelector.PortSelection portSelection = portSelector.getActuatorPort();
        if (!portSelection.hasValue() || !portSelection.getAsInteger().isPresent()) {
            LOG.error("Can't set up actuator...");
        } else {
            LOG.debug("actuatorPort {}",  portSelection);
            final Map<String, Object> map = new HashMap<>();
            map.put(ACTUATOR_ENV_KEY, portSelection.getAsInteger().getAsInt());
            final MapPropertySource mapPropertySource = new MapPropertySource("actuatorPostProcessor", map);
            environment.getPropertySources().addFirst(mapPropertySource);
        }
    }
}
