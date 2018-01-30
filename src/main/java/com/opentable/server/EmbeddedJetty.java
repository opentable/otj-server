package com.opentable.server;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;

import com.opentable.logging.jetty.JsonRequestLogConfig;

import javax.inject.Inject;
import javax.servlet.ServletContextListener;

/**
 * Configure an embedded {@code Jetty 9} HTTP(S) server, and tie it into the Spring Boot lifecycle.
 *
 * Spring Boot provides a very basic Jetty integration but it doesn't cover a lot of important use cases.
 * For example even something as trivial as configuring the worker pool size, socket options,
 * or HTTPS connector is totally unsupported.
 */
@Configuration
@Import(JsonRequestLogConfig.class)
public class EmbeddedJetty extends EmbeddedJettyBase{

    @Inject
    Optional<Collection<ServletContextListener>> listeners;

    @Bean
    public ServletWebServerFactory servletContainer(
            final JsonRequestLogConfig requestLogConfig,
            final Map<String, ServerConnectorConfig> activeConnectors,
            final PropertyResolver pr) {

        final JettyServletWebServerFactory factory = new JettyServletWebServerFactory() {

            @Override
            protected JettyWebServer getJettyWebServer(Server server) {
                // always auto-start even if the default connector isn't configured
                return new JettyWebServer(server, true);
            }
        };
        JettyWebServerFactoryAdapter factoryAdapter = new JettyWebServerFactoryAdapter(factory);
        this.configureFactoryContainer(requestLogConfig, activeConnectors, pr, factoryAdapter);

        factory.setSessionTimeout(Duration.ofMinutes(10));
        if (listeners.isPresent()) {
            factory.addInitializers(servletContext -> listeners.get().forEach(servletContext::addListener));
        }

        return factoryAdapter.getFactory();
    }

    static class JettyWebServerFactoryAdapter implements WebServerFactoryAdapter<JettyServletWebServerFactory> {
        private final JettyServletWebServerFactory factory;

        JettyWebServerFactoryAdapter(JettyServletWebServerFactory factory) {
            this.factory = factory;
        }

        @Override
        public void setPort(int port) {
            factory.setPort(port);
        }

        @Override
        public void addServerCustomizers(JettyServerCustomizer... customizers) {
            factory.addServerCustomizers(customizers);
        }

        @Override
        public void setSessionTimeout(Duration duration) {
            factory.setSessionTimeout(duration);
        }

        @Override
        public void setThreadPool(ThreadPool threadPool) {
            factory.setThreadPool(threadPool);
        }

        @Override
        public void addInitializers(ServletContextInitializer... initializers) {
            factory.addInitializers(initializers);
        }

        @Override
        public JettyServletWebServerFactory getFactory() {
            return factory;
        }

    }

}
