package com.opentable.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opentable.spring.discovery.DiscoveryClientFactory;
import com.opentable.spring.discovery.EnableDiscovery;
import com.opentable.spring.discovery.ServiceAnnouncer;

@EnableDiscovery
@Configuration
class DiscoverySetup {
    @Bean
    public DiscoveryClientFactory discoveryClientFactory() {
        return new DiscoveryClientFactory();
    }

    @Bean
    public ServiceAnnouncer serviceAnnouncer() {
        return new ServiceAnnouncer();
    }
}
