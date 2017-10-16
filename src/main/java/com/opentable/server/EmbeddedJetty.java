package com.opentable.server;

import java.time.Clock;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletContextListener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.JsonRequestLogConfig;
import com.opentable.util.Optionals;

/**
 * TODO Add all the different types of injected handlers from the old server?
 * https://github.com/opentable/otj-httpserver/blob/master/src/main/java/com/opentable/httpserver/AbstractJetty9HttpServer.java
 */
@Configuration
@Import(JsonRequestLogConfig.class)
public class EmbeddedJetty {
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedJetty.class);

    @Value("${ot.http.bind-port:${PORT0:0}}")
    private List<Integer> httpBindPort;

    @Value("${ot.httpserver.shutdown-timeout:PT5s}")
    private Duration shutdownTimeout;

    // XXX: these should be removed pending https://github.com/spring-projects/spring-boot/issues/5314
    @Value("${ot.httpserver.max-threads:32}")
    private int maxThreads;

    @Value("${ot.httpserver.min-threads:#{null}}")
    // Specifying this fails the build.
    private Integer minThreads;

    /**
     * In the case that we bind to port 0, we'll get back a port from the OS.
     * With {@link #containerInitialized(EmbeddedServletContainerInitializedEvent)}, we capture this value
     * and store it here.
     */
    private volatile Integer httpActualPort;

    @Inject
    Optional<Provider<QueuedThreadPool>> qtpProvider;

    @Inject
    Optional<Collection<ServletContextListener>> listeners;

    @Inject
    Optional<Collection<Function<Handler, Handler>>> handlerCustomizers;

    @Inject
    Optional<Collection<Consumer<Server>>> serverCustomizers;

    @Inject
    Optional<JsonRequestLog> requestLogger;

    private EmbeddedServletContainer container;

    @Bean
    public EmbeddedServletContainerFactory servletContainer(final JsonRequestLogConfig requestLogConfig) {
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

            if (!requestLogConfig.isEnabled()) {
                LOG.debug("request logging disabled; config {}", requestLogConfig);
            } else {
                final RequestLogHandler logHandler = new RequestLogHandler();
                logHandler.setRequestLog(requestLogger.orElseGet(
                        () -> new JsonRequestLog(Clock.systemUTC(), requestLogConfig)));
                logHandler.setHandler(customizedHandler);
                customizedHandler = logHandler;
                LOG.debug("request logging enabled; added log handler with config {}", requestLogConfig);
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
        factory.addServerCustomizers(this::sizeThreadPool);
        factory.addServerCustomizers(server ->
            Optionals.stream(serverCustomizers).flatMap(Collection::stream).forEach(customizer -> {
                LOG.debug("Customizing server {} with {}", server, customizer);
                customizer.accept(server);
            }));

        return factory;
    }

    private void sizeThreadPool(Server server) {
        Verify.verify(minThreads == null, "'ot.httpserver.min-threads' has been removed on the " +
                "theory that it is always preferable to eagerly initialize worker threads " +
                "instead of doing so lazily and finding out you can't allocate thread stacks. " +
                "Talk to Platform Architecture if you think you need this tuneable.");

        final QueuedThreadPool qtp = (QueuedThreadPool) server.getThreadPool();
        qtp.setMinThreads(maxThreads);
        qtp.setMaxThreads(maxThreads);
    }

    @EventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
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

    @Bean
    public HttpServerInfo httpServerInfo() {
        return new HttpServerInfo() {
            @Override
            public int getPort() {
                // Safe because state of httpActualPort can only go from null => non null
                Preconditions.checkState(httpActualPort != null, "http port not yet initialized");
                return httpActualPort;
            }

            @Override
            public int getPoolSize() {
                return maxThreads;
            }
        };
    }

    @VisibleForTesting
    @Bean
    @Lazy
    Server getServer() {
        Preconditions.checkState(container != null, "container not yet available");
        return ((JettyEmbeddedServletContainer) container).getServer();
    }

    @VisibleForTesting
    QueuedThreadPool getThreadPool() {
        return (QueuedThreadPool) getServer().getThreadPool();
    }
}
