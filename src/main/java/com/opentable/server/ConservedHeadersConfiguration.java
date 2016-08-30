package com.opentable.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.conservedheaders.ClientConservedHeadersFeature;
import com.opentable.jaxrs.JaxRsFeatureBinding;
import com.opentable.jaxrs.StandardFeatureGroup;

@Configuration
@Import(com.opentable.conservedheaders.ConservedHeadersConfiguration.class)
public class ConservedHeadersConfiguration {
    @Bean
    JaxRsFeatureBinding getConserveHeadersFeatureBinding(final ClientConservedHeadersFeature feature) {
        return JaxRsFeatureBinding.bind(StandardFeatureGroup.PLATFORM_INTERNAL, feature);
    }
}
