package com.opentable.server;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Provider;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddedJetty {
    @Value("${ot.http.bind-port:${PORT0:0}}")
    int httpBindPort;

    @Bean
    public EmbeddedServletContainerFactory servletContainer(
            final Optional<Provider<QueuedThreadPool>> qtpProvider,
            final Collection<ServletContextListener> listeners,
            final Collection<Function<Handler, Handler>> handlerCustomizers) {
        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.setPort(httpBindPort);
        factory.setSessionTimeout(10, TimeUnit.MINUTES);
        if (qtpProvider.isPresent()) {
            factory.setThreadPool(qtpProvider.get().get());
        }
        factory.addInitializers(servletContext -> listeners.forEach(servletContext::addListener));
        factory.addServerCustomizers(server -> {
            Handler customizedHandler = new StatisticsHandler();
            for (final Function<Handler, Handler> customizer : handlerCustomizers) {
                customizedHandler = customizer.apply(customizedHandler);
            }
            server.setHandler(customizedHandler);
        });
        return factory;
    }

    @Bean
    public HttpServerInfo serverInfo() {
        return new HttpServerInfo(httpBindPort);
    }
}
