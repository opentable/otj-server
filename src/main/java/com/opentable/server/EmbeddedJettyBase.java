package com.opentable.server;

import java.io.IOException;
import java.security.Principal;
import java.security.cert.CRL;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.ClientCertAuthenticator;
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
import org.eclipse.jetty.server.UserIdentity;
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.opentable.jaxrs.TlsProvider;
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
@Configuration
@Import(JsonRequestLogConfig.class)
public abstract class EmbeddedJettyBase {
    public static final String DEFAULT_CONNECTOR_NAME = "default-http";
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
    Optional<TlsProvider> tlsProvider;

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

        final PortIterator ports = new PortIterator(pr);
        final ImmutableMap.Builder<String, ConnectorInfo> connectorInfos = ImmutableMap.builder();
        final ServerConnectorConfig defaultConnector = activeConnectors.get(DEFAULT_CONNECTOR_NAME);

        // Remove Spring Boot's gimped default connector, we'll make a better one
        factory.addServerCustomizers(server -> server.setConnectors(new Connector[0]));
        if (defaultConnector == null) {
            LOG.debug("Disabling default HTTP connector");
            factory.setPort(0);
        }
        if (qtpProvider.isPresent()) {
            factory.setThreadPool(qtpProvider.get().get());
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
            final StatisticsHandler stats = new StatisticsHandler();
            stats.setHandler(customizedHandler);

            if (tlsProvider.isPresent()) {
                final ConstraintSecurityHandler security = new ConstraintSecurityHandler();
                security.setLoginService(new CredentialsManagementLoginService());
                security.setAuthenticator(new ClientCertAuthenticator());
                security.setHandler(stats);

                server.setHandler(security);
            } else {
                server.setHandler(stats);
            }

            activeConnectors.forEach((name, config) -> {
                connectorInfos.put(name, createConnector(server, name, ports, config));
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
    }

    @SuppressWarnings("resource")
    @SuppressFBWarnings("SF_SWITCH_FALLTHROUGH")
    private ConnectorInfo createConnector(Server server, String name, IntSupplier port, ServerConnectorConfig config) {
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
        if (ssl != null || tlsProvider.isPresent()) {
            httpConfig.addCustomizer(new SecureRequestCustomizer());
        } else if (config.isForceSecure()) {
            // Used when SSL is terminated externally, e.g. by nginx or elb
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
        connector.setHost(config.getBindAddress());
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

    class SuperSadSslContextFactory extends SslContextFactory {
        private volatile CRL crl;

        SuperSadSslContextFactory(String name, ServerConnectorConfig config) {
            super(config.getKeystore());
            if (config.getKeystore() != null) {
                // Keystore manually set in config
                setKeyStorePassword(config.getKeystorePassword());
            } else {
                final TlsProvider tls = tlsProvider
                    .orElseThrow(() -> new IllegalStateException("no keystore specified for '" + name + "'"));
                // Enable rotating TLS support
                tls.init((trustStore, keyStore) -> {
                    // Important note regarding TLS session resumption (not-yet-determined impact to us):
                    // https://github.com/eclipse/jetty.project/issues/918
                    // Possible workaround outlined at:
                    // https://github.com/eclipse/jetty.project/issues/519
                    try {
                        crl = tls.crl();
                        reload(f -> {
                            f.setWantClientAuth(true);
                            f.setValidateCerts(true);
                            f.setValidatePeerCerts(true);
                            f.setKeyStorePassword("");
                            f.setTrustStorePassword("");
                            f.setTrustStore(trustStore);
                            f.setKeyStore(keyStore);
                        });
                    } catch (Exception e) {
                        Throwables.throwIfUnchecked(e);
                        throw new RuntimeException(e);
                    }
                });
            }
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

        @Override
        protected Collection<? extends CRL> loadCRL(String crlPath) throws Exception {
            return Collections.singleton(crl);
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

    static class CredentialsManagementLoginService implements LoginService {
        private IdentityService identity = new DefaultIdentityService();
        @Override
        public String getName() {
            return "otj-credentials";
        }

        @Override
        public UserIdentity login(String username, Object credentials, ServletRequest request) {
            return new CredentialsManagementUserIdentity(username, request);
        }

        @Override
        public boolean validate(UserIdentity user) {
            return true;
        }

        @Override
        public IdentityService getIdentityService() {
            return identity;
        }

        @Override
        public void setIdentityService(IdentityService identity) {
            this.identity = identity;
        }

        @Override
        public void logout(UserIdentity user) {

        }
    }

    static class CredentialsManagementUserIdentity implements UserIdentity {
        private final String username;

        CredentialsManagementUserIdentity(String username, ServletRequest request) {
            this.username = username;
        }

        @Override
        public Subject getSubject() {
            return new Subject(true, Collections.singleton(getUserPrincipal()), Collections.emptySet(), Collections.emptySet());
        }

        @Override
        public Principal getUserPrincipal() {
            return new CredentialsManagementUserPrincipal(username);
        }

        @Override
        public boolean isUserInRole(String role, Scope scope) {
            throw new UnsupportedOperationException("TODO: Roles not yet implemented");
        }
    }

    static class CredentialsManagementUserPrincipal implements Principal {
        private final String username;

        CredentialsManagementUserPrincipal(String username) {
            this.username = StringUtils.removeStart(username, "CN=");
        }

        @Override
        public String getName() {
            return username;
        }
    }
}
