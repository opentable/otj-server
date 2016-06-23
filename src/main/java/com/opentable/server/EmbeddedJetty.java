package com.opentable.server;

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
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;

/**
 * TODO Add all the different types of injected handlers from the old server?
 * https://github.com/opentable/otj-httpserver/blob/master/src/main/java/com/opentable/httpserver/AbstractJetty9HttpServer.java
 */
@Configuration
public class EmbeddedJetty {
    @Value("${ot.http.bind-port:${PORT0:0}}")
    private List<Integer> httpBindPort;

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
            server.setHandler(customizedHandler);

            for (int i = 1; i < httpBindPort.size(); i++) {
                final ServerConnector connector = new ServerConnector(server);
                connector.setPort(httpBindPort.get(i));
                server.addConnector(connector);
            }
        });
        return factory;
    }

    @EventListener
    public void containerInitialized(final EmbeddedServletContainerInitializedEvent evt) {
        final int port = evt.getEmbeddedServletContainer().getPort();
        if (port != -1) {
            httpActualPort = port;
        }
    }

    @Lazy
    @Bean
    public HttpServerInfo serverInfo() {
        Preconditions.checkState(httpActualPort != null, "http port not yet initialized");
        return new HttpServerInfo(httpActualPort);
    }
}
