package com.opentable.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.jackson.OpenTableJacksonConfiguration;
import com.opentable.jaxrs.JaxRsClientConfiguration;
import com.opentable.metrics.DefaultMetricsConfiguration;
import com.opentable.metrics.http.HealthHttpConfiguration;
import com.opentable.metrics.http.MetricsHttpConfiguration;
import com.opentable.pausedetector.EnablePauseDetector;

/**
 * REST HTTP Server.
 *
 * @see ServerLoggingConfiguration for its special setup.
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
    EmbeddedJetty.class,
    BackendInfoFilterConfiguration.class,
    ConservedHeadersConfiguration.class,
    ResteasyAutoConfiguration.class,
    OpenTableJacksonConfiguration.class,
    JaxRsClientConfiguration.class,
    ServerConfigConfiguration.class,
    StaticResourceConfiguration.class,
    JmxConfiguration.class,
    DefaultMetricsConfiguration.class,
    HealthHttpConfiguration.class,
    MetricsHttpConfiguration.class,
    StartupShutdownFailedHandler.class,
})
@EnablePauseDetector
public @interface RestHttpServer {
}
