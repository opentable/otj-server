package com.opentable.server.reactive.logging;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.JsonRequestLogConfig;

/**
 * Configures automatic server request logging using Jetty for reactive applications.
 */
@Configuration
public class ReactiveServerLoggingConfiguration {

    /**
     * Override the bean injected in to EmbeddedJettyBase with our own configuration.
     */
    @Bean
    public JsonRequestLog requestLogger(JsonRequestLogConfig config) {
        return new ServerRequestLog(Clock.systemUTC(), config);
    }
}
