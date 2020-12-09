/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.server;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.management.MBeanServer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;

import org.eclipse.jetty.jmx.MBeanContainer;
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
import org.eclipse.jetty.util.thread.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertyResolver;
import org.springframework.util.CollectionUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.JsonRequestLogConfig;
import com.opentable.server.HttpServerInfo.ConnectorInfo;
import com.opentable.spring.SpecializedConfigFactory;
import com.opentable.util.Optionals;

/**
 * base class providing common configuration for creating Embedded Jetty instances
 * Spring Boot provides a very basic Jetty integration but it doesn't cover a lot of important use cases.
 * For example even something as trivial as configuring the worker pool size, socket options,
 * or HTTPS connector is totally unsupported.
 */
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
@Configuration
@Import(JsonRequestLogConfig.class)
public abstract class EmbeddedJettyBase {
    public static final String DEFAULT_CONNECTOR_NAME = "default-http";
    public static final String BOOT_CONNECTOR_NAME = "boot";
    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedJettyBase.class);

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

    @Value("${ot.httpserver.ssl-excluded-protocols:}")
    List<String> excludedProtocols;

    // the following two values (shouldSleepBeforeShutdown, sleepDurationBeforeShutdown) help an application control whether they want a pause before jetty shutdown to ensure that the discovery unannounce has propagated to all clients requesting the application
    @Value("${ot.httpserver.sleep-before-shutdown:#{false}}")
    boolean shouldSleepBeforeShutdown;

    @Value("${ot.httpserver.sleep-duration-before-shutdown:PT5s}")
    Duration sleepDurationBeforeShutdown;

    @Value("${ot.httpserver.show-stack-on-error:#{environment.acceptsProfiles(\"!deployed\")}}")
    boolean showStacks;

    /**
     * In the case that we bind to port 0, we'll get back a port from the OS.
     * With {@link #containerInitialized(WebServerInitializedEvent)}, we capture this value
     * and store it here.
     */
    private volatile Integer httpActualPort;

    @Inject
    Optional<Provider<QueuedThreadPool>> qtpProvider;

    @Inject
    Optional<Collection<Function<Handler, Handler>>> handlerCustomizers;

    @Inject
    Optional<Collection<Consumer<Server>>> serverCustomizers;

    @Inject
    Optional<Collection<Consumer<HttpConfiguration>>> httpConfigCustomizers;

    @Inject
    Optional<JsonRequestLog> requestLogger;

    @Inject
    Optional<MBeanServer> mbs;

    private Map<String, ConnectorInfo> connectorInfos;

    @Bean
    Map<String, ServerConnectorConfig> activeConnectors(SpecializedConfigFactory<ServerConnectorConfig> configFactory) {
        final ImmutableMap.Builder<String, ServerConnectorConfig> builder = ImmutableMap.builder();
        activeConnectors.forEach(name -> builder.put(name, configFactory.getConfig(name)));

        final ImmutableMap<String, ServerConnectorConfig> result = builder.build();
        LOG.info("Built active connector list: {}", result);
        return result;
    }


    protected void configureFactoryContainer(
            final JsonRequestLogConfig requestLogConfig,
            final Map<String, ServerConnectorConfig> activeConnectors,
            final PropertyResolver pr,
            final WebServerFactoryAdapter<?> factory) {
        if (httpBindPort != null) {
            throw new IllegalStateException("'ot.http.bind-port' is deprecated, refer to otj-server README for replacement");
        }

        final ImmutableMap.Builder<String, ConnectorInfo> connectorInfos = ImmutableMap.builder();
        final ServerConnectorConfig defaultConnector = activeConnectors.get(DEFAULT_CONNECTOR_NAME);

        if (defaultConnector == null) {
            LOG.debug("Disabling default HTTP connector");
            factory.setPort(0);
        }
        if (qtpProvider.isPresent()) {
            factory.setThreadPool(qtpProvider.get().get());
        }
        factory.addServerCustomizers(server -> {
            mbs.ifPresent(m -> server.addBean(new MBeanContainer(m)));
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
            final StatisticsHandler stats = new StatisticsHandler();
            stats.setHandler(customizedHandler);
            server.setHandler(stats);

            // Get Spring Boot's default connector, so we can get properties from it
            ServerConnector bootConnector = Arrays.stream(server.getConnectors())
                .filter(i -> i instanceof ServerConnector)
                .map(i -> (ServerConnector) i)
                .findFirst()
                .orElse(null);

            // Remove Spring Boot's gimped default connector, we'll make a better one
            server.setConnectors(new Connector[0]);

            activeConnectors.forEach((name, config) -> {
                connectorInfos.put(name, createConnector(server, name, config, bootConnector, pr));
            });
            bootConnector.close();
            this.connectorInfos = connectorInfos.build();

            server.setStopTimeout(shutdownTimeout.toMillis());
        });
        factory.addServerCustomizers(this::sizeThreadPool);
        factory.addServerCustomizers(server ->
                Optionals.stream(serverCustomizers).flatMap(Collection::stream).forEach(customizer -> {
                    LOG.debug("Customizing server {} with {}", server, customizer);
                    customizer.accept(server);
                }));
    }

    @SuppressWarnings("resource")
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    private ConnectorInfo createConnector(Server server, String name,  ServerConnectorConfig config, ServerConnector bootConnector, PropertyResolver pr) {
        final List<ConnectionFactory> factories = new ArrayList<>();

        final SslContextFactory ssl;

        switch (config.getProtocol()) { // NOPMD
            case "proxy+http":
                factories.add(new ProxyConnectionFactory());
                //$FALL-THROUGH$
            case "http":
                ssl = null;
                break;
            case "proxy+https":
                factories.add(new ProxyConnectionFactory());
                //$FALL-THROUGH$
            case "https":
                ssl = new SuperSadSslContextFactory(name, config);
                break;
            default:
                throw new UnsupportedOperationException(String.format("For connector '%s', unsupported protocol '%s'", name, config.getProtocol()));
        }

        final HttpConfiguration httpConfig = new HttpConfiguration();

        // Don't reveal these
        httpConfig.setSendServerVersion(false);
        httpConfig.setSendXPoweredBy(false);

        if (ssl != null) {
            httpConfig.addCustomizer(new SecureRequestCustomizer());
        } else if (config.isForceSecure()) {
            // Used when SSL is terminated externally, e.g. by nginx or elb
            httpConfig.addCustomizer(new SuperSecureCustomizer());
        }
        httpConfigCustomizers.ifPresent(c -> c.forEach(h -> h.accept(httpConfig)));
        final HttpConnectionFactory http = new HttpConnectionFactory(httpConfig);

        if (ssl != null) {
            if (!CollectionUtils.isEmpty(excludedProtocols)) {
                LOG.warn("Excluding following protocols:");
                excludedProtocols.forEach(protocol -> LOG.warn("Disabling {}", protocol));
                ssl.setExcludeProtocols(excludedProtocols.toArray(new String[0]));
            }

            factories.add(new SslConnectionFactory(ssl, http.getProtocol()));
        }

        factories.add(http);

        @SuppressWarnings("PMD.CloseResource")
        final ServerConnector connector = new ServerConnector(server,
                factories.toArray(new ConnectionFactory[factories.size()]));
        connector.setName(name);
        if (BOOT_CONNECTOR_NAME.equals(name) && bootConnector != null) {
            connector.setHost(bootConnector.getHost());
            connector.setPort(Integer.parseInt(pr.getProperty("server.port", "8080").trim()));
            LOG.debug("Configuring HTTP connector, setting host and port to Spring's defaults.");
        } else {
            connector.setHost(config.getBindAddress());
            connector.setPort(selectPort(config));
        }
        if (config.getIdleTimeout() > 0) {
            connector.setIdleTimeout(config.getIdleTimeout());
        }

        server.addConnector(connector);
        return new ServerConnectorInfo(name, connector, config);
    }

    private int selectPort(ServerConnectorConfig connectorConfig) {
        int configuredPort = connectorConfig.getPort();
        if (configuredPort < 0) {
            return 0;
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
    public void containerInitialized(final WebServerInitializedEvent evt) throws IOException {
        WebServer container = evt.getWebServer();
        serverHolder().set(container);
        final int port = container.getPort();
        if (port > 0) {
            httpActualPort = port;
        }

        LOG.info("WebServer initialized; pool={}", getThreadPool());
        if (LOG.isTraceEnabled()) {
            final StringBuilder dump = new StringBuilder();
            getServer().dump(dump, "  ");
            LOG.trace("Server configuration: {}", dump);
        }
    }

    // XXX: this is a workaround for
    // https://github.com/spring-projects/spring-boot/issues/4657
    @EventListener
    public void gracefulShutdown(ContextClosedEvent evt) {
        WebServer container = serverHolder().get();
        LOG.debug("Received application context closed event {}. Shutting down...", evt);
        LOG.info("Early shutdown of Jetty connectors on {}", container);
        if (container != null) {
            if(shouldSleepBeforeShutdown) {
                long sleepDurationMillisBeforeShutdown = sleepDurationBeforeShutdown.toMillis();
                LOG.info("Application config requesting sleep for {} ms before Jetty shutdown", sleepDurationMillisBeforeShutdown);
                sleepBeforeJettyShutdown(sleepDurationMillisBeforeShutdown);
            }
            container.stop();
            LOG.info("Jetty is stopped.");
        } else {
            LOG.warn("Never got a Jetty?");
        }
    }

    private void sleepBeforeJettyShutdown(long sleepDurationMillisBeforeShutdown) {
        try {
            LOG.info("Sleeping for {} ms before jetty shutdown", sleepDurationMillisBeforeShutdown);
            Thread.sleep(sleepDurationMillisBeforeShutdown);
        } catch (InterruptedException e) {
            LOG.error("Failed to sleep before shutdown {}", e);
            Thread.currentThread().interrupt();
        } finally {
            LOG.info("Sleep complete, preparing to shut down jetty.");
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

    @Bean
    AtomicReference<WebServer> serverHolder() {
        return new AtomicReference<>();
    }

    @VisibleForTesting
    @Bean
    @Lazy
    Server getServer() {
        final WebServer container = serverHolder().get();
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

    class SuperSadSslContextFactory extends SslContextFactory.Server {
        SuperSadSslContextFactory(String name, ServerConnectorConfig config) {
            Preconditions.checkState(config.getKeystore() != null, "no keystore specified for '%s'", name);
            setKeyStorePath(config.getKeystore());
            setKeyStorePassword(config.getKeystorePassword());
        }

        @Override
        protected void removeExcludedCipherSuites(List<String> selectedCiphers) {
            super.removeExcludedCipherSuites(selectedCiphers);

            if (allowedDeprecatedCiphers.isEmpty()) {
                return;
            }

            LOG.warn("***************************************************************************************");
            LOG.warn("TEMPORARILY ALLOWING VULNERABLE, DEPRECATED SSL CIPHERS FOR BUSTED CLIENTS!!!");
            allowedDeprecatedCiphers.forEach(cipher ->
                    LOG.warn("    * {}", cipher)
            );
            LOG.warn("***************************************************************************************");
            selectedCiphers.addAll(allowedDeprecatedCiphers);
        }
    }

    interface WebServerFactoryAdapter<T> {

        void setPort(int port);

        void addServerCustomizers(JettyServerCustomizer... customizers);

        void setSessionTimeout(Duration duration);

        void setThreadPool(ThreadPool threadPool);

        void addInitializers(ServletContextInitializer... initializers);

        T getFactory();
    }
}
