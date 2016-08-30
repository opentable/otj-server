package com.opentable.server;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletContextListener;

import com.google.common.base.Preconditions;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;

/**
 * TODO Add all the different types of injected handlers from the old server?
 * https://github.com/opentable/otj-httpserver/blob/master/src/main/java/com/opentable/httpserver/AbstractJetty9HttpServer.java
 */
@Configuration
public class EmbeddedJetty {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedJetty.class);

    @Value("${ot.http.bind-port:${PORT0:0}}")
    private List<Integer> httpBindPort;

    @Value("${ot.httpserver.shutdown-timeout:PT5s}")
    private Duration shutdownTimeout;

    /**
     * In the case that we bind to port 0, we'll get back a port from the OS.
     * With {@link #containerInitialized(EmbeddedServletContainerInitializedEvent)}, we capture this value
     * and store it here.
     */
    private Integer httpActualPort;

    @Inject
    Optional<Provider<QueuedThreadPool>> qtpProvider;

    @Inject
    Optional<Collection<ServletContextListener>> listeners;

    @Inject
    Optional<Collection<Function<Handler, Handler>>> handlerCustomizers;

    private EmbeddedServletContainer container;

    @Bean
    public EmbeddedServletContainerFactory servletContainer() {
        if (httpBindPort.isEmpty()) {
            throw new IllegalStateException("Must specify at least one 'ot.http.bind-port'");
        }

        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.setPort(httpBindPort.get(0));
        factory.setSessionTimeout(10, TimeUnit.MINUTES);
        if (qtpProvider.isPresent()) {
            factory.setThreadPool(qtpProvider.get().get());
        }
        if (listeners.isPresent()) {
            factory.addInitializers(servletContext -> listeners.get().forEach(servletContext::addListener));
        }
        factory.addServerCustomizers(server -> {
            Handler customizedHandler = server.getHandler();
            if (handlerCustomizers.isPresent()) {
                for (final Function<Handler, Handler> customizer : handlerCustomizers.get()) {
                    customizedHandler = customizer.apply(customizedHandler);
                }
            }

            // Required for graceful shutdown to work
            StatisticsHandler stats = new StatisticsHandler();
            stats.setHandler(customizedHandler);

            server.setHandler(stats);

            for (int i = 1; i < httpBindPort.size(); i++) {
                final ServerConnector connector = new ServerConnector(server);
                connector.setPort(httpBindPort.get(i));
                server.addConnector(connector);
            }

            server.setStopTimeout(shutdownTimeout.toMillis());
        });
        return factory;
    }

    @EventListener
    public void containerInitialized(final EmbeddedServletContainerInitializedEvent evt) {
        container = evt.getEmbeddedServletContainer();
        final int port = container.getPort();
        if (port != -1) {
            httpActualPort = port;
        }
    }

    // XXX: this is a workaround for
    // https://github.com/spring-projects/spring-boot/issues/4657
    @EventListener
    public void gracefulShutdown(ContextClosedEvent evt) {
        LOG.info("Early shutdown of Jetty connectors");
        if (container != null) {
            container.stop();
            LOG.info("Jetty is stopped.");
        } else {
            LOG.warn("Never got a Jetty?");
        }
    }

    @Lazy
    @Bean
    public HttpServerInfo serverInfo() {
        Preconditions.checkState(httpActualPort != null, "http port not yet initialized");
        return new HttpServerInfo(httpActualPort);
    }
}
