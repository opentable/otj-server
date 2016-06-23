package com.opentable.server;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.metrics.DefaultMetricsConfiguration;
import com.opentable.metrics.http.HealthHttpConfiguration;

@Configuration
@Import({
        DefaultMetricsConfiguration.class,
        HealthHttpConfiguration.class,
})
public class MetricsConfiguration {
    @Bean
    public MetricRegistry getMetrics() {
        return new MetricRegistry();
    }

    @Bean
    public HealthCheckRegistry getHealthCheckRegistry() {
        return new HealthCheckRegistry();
    }
}
