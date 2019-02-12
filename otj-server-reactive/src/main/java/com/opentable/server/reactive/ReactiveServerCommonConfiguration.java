package com.opentable.server.reactive;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.metrics.reactive.HealthHttpReactiveConfiguration;
import com.opentable.metrics.reactive.MetricsHttpReactiveConfiguration;
import com.opentable.server.EmbeddedJettyConfiguration;
import com.opentable.server.EmbeddedReactiveJetty;
import com.opentable.server.NonWebSetup;
import com.opentable.server.reactive.logging.ReactiveServerLoggingConfiguration;
import com.opentable.server.reactive.webfilter.BackendInfoWebFilterConfiguration;

/**
 * Common configuration for Spring WebFlux reactive servers.
 */
@Configuration
@EnableConfigurationProperties
@Import({
        // OT Embedded Jetty
        EmbeddedJettyConfiguration.class,
        EmbeddedReactiveJetty.class,
        // Log server request information
        ReactiveServerLoggingConfiguration.class,
        // Default health check
        HealthHttpReactiveConfiguration.class,
        // Metrics for http
        MetricsHttpReactiveConfiguration.class,
        // Filter for transfer of core backend info
        BackendInfoWebFilterConfiguration.class,

        // Support static resources
        // TODO: Need to test serving static resources the WebFlux way
})
@ImportAutoConfiguration({
        // Core WebFlux
        ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        ErrorWebFluxAutoConfiguration.class,
        WebFluxAutoConfiguration.class,
        HttpHandlerAutoConfiguration.class,
})
@NonWebSetup
class ReactiveServerCommonConfiguration {
}
