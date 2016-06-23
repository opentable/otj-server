package com.opentable.server;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.opentable.metrics.http.MetricsHttpConfiguration;

/**
 * Demonstration of serving Dropwizard Metrics AdminServlet.
 * Navigate to /metrics.
 */
@Configuration
@Import({
        TestServer.class,
        MetricsHttpConfiguration.class,
})
@PropertySource("metricsdemo.properties")
public class MetricsDemo {
    public static void main(final String[] args) {
        SpringApplication.run(MetricsDemo.class, args);
    }
}
