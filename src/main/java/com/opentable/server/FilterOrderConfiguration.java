package com.opentable.server;

import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.components.filterorder.FilterOrderResolverConfiguration;
import com.opentable.components.filterorder.OrderDeclaration;

@Configuration
@Import(FilterOrderResolverConfiguration.class)
public class FilterOrderConfiguration {
    @Bean
    public OrderDeclaration filter30OrderDeclaration() {
        return OrderDeclaration.last(Filter30Dispatcher.class);
    }
}
