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
    /**
     * {@link org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher} must come last because it is the filter
     * that does the <em>actual</em> work of handling the request; it is thus terminal.
     */
    @Bean
    public OrderDeclaration filter30OrderDeclaration() {
        return OrderDeclaration.last(Filter30Dispatcher.class);
    }
}
