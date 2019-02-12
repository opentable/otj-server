package com.opentable.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;

import com.opentable.spring.SpecializedConfigFactory;

/**
 * Provide beans required to construct implementations of {@link EmbeddedJettyBase}.
 */
@Configuration
public class EmbeddedJettyConfiguration {

    @Bean
    public SpecializedConfigFactory<ServerConnectorConfig> connectorConfigs(PropertyResolver pr) {
        return SpecializedConfigFactory.create(pr, ServerConnectorConfig.class, "ot.httpserver.connector.${name}");
    }
}
