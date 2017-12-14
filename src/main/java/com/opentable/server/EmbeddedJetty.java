package com.opentable.server;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.ServletContextListener;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;

import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.ProxyConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertyResolver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.JsonRequestLogConfig;
import com.opentable.server.HttpServerInfo.ConnectorInfo;
import com.opentable.spring.SpecializedConfigFactory;
import com.opentable.util.Optionals;

/**
 * Configure an embedded {@code Jetty 9} HTTP(S) server, and tie it into the Spring Boot lifecycle.
 *
 * Spring Boot provides a very basic Jetty integration but it doesn't cover a lot of important use cases.
 * For example even something as trivial as configuring the worker pool size, socket options,
 * or HTTPS connector is totally unsupported.
 */
@Configuration
@Import(JsonRequestLogConfig.class)
public class EmbeddedJetty {
    public static final String DEFAULT_CONNECTOR_NAME = "default-http";
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedJetty.class);

    @Value("${ot.http.bind-port:#{null}}")
    // Specifying this fails startup
    private String httpBindPort;

    @Value("${ot.httpserver.shutdown-timeout:PT5s}")
    private Duration shutdownTimeout;

    // XXX: these should be removed pending https://github.com/spring-projects/spring-boot/issues/5314
    @Value("${ot.httpserver.max-threads:32}")
    private int maxThreads;

    @Value("${ot.httpserver.min-threads:#{null}}")
    // Specifying this fails the build.
    private Integer minThreads;

    @Value("${ot.httpserver.active-connectors:default-http}")
    List<String> activeConnectors;

    @Value("${ot.httpserver.ssl-allowed-deprecated-ciphers:}")
    List<String> allowedDeprecatedCiphers;

    /**
     * In the case that we bind to port 0, we'll get back a port from the OS.
     * With {@link #containerInitialized(WebServerInitializedEvent)}, we capture this value
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
    Optional<Collection<Consumer<HttpConfiguration>>> httpConfigCustomizers;

    @Inject
    Optional<JsonRequestLog> requestLogger;

    private WebServer container;

    private Map<String, ConnectorInfo> connectorInfos;

    @Bean
    Map<String, ServerConnectorConfig> activeConnectors(SpecializedConfigFactory<ServerConnectorConfig> configFactory) {
        final ImmutableMap.Builder<String, ServerConnectorConfig> builder = ImmutableMap.builder();
        activeConnectors.forEach(name -> builder.put(name, configFactory.getConfig(name)));

        final ImmutableMap<String, ServerConnectorConfig> result = builder.build();
        LOG.info("Built active connector list: {}", result);
        return result;
    }

    @Bean
    public ServletWebServerFactory servletContainer(
            final JsonRequestLogConfig requestLogConfig,
            final Map<String, ServerConnectorConfig> activeConnectors,
            final PropertyResolver pr)
    {
        if (httpBindPort != null) {
            throw new IllegalStateException("'ot.http.bind-port' is deprecated, refer to otj-server README for replacement");
        }

        final PortIterator ports = new PortIterator(pr);
        final ImmutableMap.Builder<String, ConnectorInfo> connectorInfos = ImmutableMap.builder();
        final JettyServletWebServerFactory factory = new JettyServletWebServerFactory() {

            @Override
            protected JettyWebServer getJettyWebServer(Server server) {
                // always auto-start even if the default connector isn't configured
                return new JettyWebServer(server, true);
            }
        };
        final ServerConnectorConfig defaultConnector = activeConnectors.get(DEFAULT_CONNECTOR_NAME);

        if (defaultConnector != null) {
            factory.setPort(selectPort(ports, defaultConnector));
            Preconditions.checkArgument(!defaultConnector.isForceSecure(), DEFAULT_CONNECTOR_NAME + " may not set secure");
            Preconditions.checkArgument(defaultConnector.getProtocol().equals("http"), DEFAULT_CONNECTOR_NAME + " may not set protocol");
            connectorInfos.put(DEFAULT_CONNECTOR_NAME, new DefaultHttpConnectorInfo(this));
            httpConfigCustomizers.ifPresent(customizers ->
                factory.addServerCustomizers(server ->
                    customizers.forEach(customizer ->
                        (server.getConnectors()[0]).getConnectionFactories().stream()
                                .filter(HttpConnectionFactory.class::isInstance)
                                .map(HttpConnectionFactory.class::cast)
                                .map(HttpConnectionFactory::getHttpConfiguration)
                                .forEach(customizer))));
        } else {
            LOG.debug("Disabling default HTTP connector");
            factory.setPort(0);
            factory.addServerCustomizers(server -> server.setConnectors(new Connector[0]));
        }
        factory.setSessionTimeout(Duration.ofMinutes(10));
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

            activeConnectors.forEach((name, config) -> {
                if (!name.equals(DEFAULT_CONNECTOR_NAME)) {
                    connectorInfos.put(name, createConnector(server, name, ports, config));
                }
            });
            this.connectorInfos = connectorInfos.build();

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

    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    private ConnectorInfo createConnector(Server server, String name, IntSupplier port, ServerConnectorConfig config) {
        final List<ConnectionFactory> factories = new ArrayList<>();

        final SslContextFactory ssl;

        switch (config.getProtocol()) { // NOPMD
        case "proxy+http":
            factories.add(new ProxyConnectionFactory());
        case "http":
            ssl = null;
            break;
        case "proxy+https":
            factories.add(new ProxyConnectionFactory());
        case "https":
            ssl = new SuperSadSslContextFactory(name, config);
            break;
        default:
            throw new UnsupportedOperationException(String.format("For connector '%s', unsupported protocol '%s'", name, config.getProtocol()));
        }

        final HttpConfiguration httpConfig = new HttpConfiguration();
        if (ssl != null) {
            httpConfig.addCustomizer(new SecureRequestCustomizer());
        } else if (config.isForceSecure()) {
            httpConfig.addCustomizer(new SuperSecureCustomizer());
        }
        httpConfigCustomizers.ifPresent(c -> c.forEach(h -> h.accept(httpConfig)));
        final HttpConnectionFactory http = new HttpConnectionFactory(httpConfig);

        if (ssl != null) {
            factories.add(new SslConnectionFactory(ssl, http.getProtocol()));
        }

        factories.add(http);

        final ServerConnector connector = new ServerConnector(server,
                factories.toArray(new ConnectionFactory[factories.size()]));
        connector.setName(name);
        connector.setPort(selectPort(port, config));

        server.addConnector(connector);
        return new ServerConnectorInfo(name, connector, config);
    }

    private int selectPort(IntSupplier nextAssignedPort, ServerConnectorConfig connectorConfig) {
        int configuredPort = connectorConfig.getPort();
        if (configuredPort < 0) {
            return nextAssignedPort.getAsInt();
        }
        return configuredPort;
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
    public void containerInitialized(final WebServerInitializedEvent evt) {
        container = evt.getWebServer();
        final int port = container.getPort();
        if (port > 0) {
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
                return getDefaultHttpActualPort();
            }

            @Override
            public Map<String, ConnectorInfo> getConnectors() {
                Preconditions.checkState(connectorInfos != null, "connector info not available yet, please wait for Jetty to be ready");
                return connectorInfos;
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
        return ((JettyWebServer) container).getServer();
    }

    @VisibleForTesting
    QueuedThreadPool getThreadPool() {
        return (QueuedThreadPool) getServer().getThreadPool();
    }

    int getDefaultHttpActualPort() {
        // Safe because state of httpActualPort can only go from null => non null
        Preconditions.checkState(httpActualPort != null, "default connector http port not initialized");
        return httpActualPort;
    }

    class SuperSadSslContextFactory extends SslContextFactory {
        SuperSadSslContextFactory(String name, ServerConnectorConfig config) {
            super(config.getKeystore());
            Preconditions.checkState(config.getKeystore() != null, "no keystore specified for '%s'", name);
            setKeyStorePassword(config.getKeystorePassword());
        }

        @Override
        protected void removeExcludedCipherSuites(List<String> selected_ciphers) {
            super.removeExcludedCipherSuites(selected_ciphers);

            if (allowedDeprecatedCiphers.isEmpty()) {
                return;
            }

            LOG.warn("***************************************************************************************");
            LOG.warn("TEMPORARILY ALLOWING VULNERABLE, DEPRECATED SSL CIPHERS FOR BUSTED CLIENTS!!!");
            allowedDeprecatedCiphers.forEach(cipher ->
                    LOG.warn("    * {}", cipher)
            );
            LOG.warn("***************************************************************************************");
            selected_ciphers.addAll(allowedDeprecatedCiphers);
        }
    }
}
