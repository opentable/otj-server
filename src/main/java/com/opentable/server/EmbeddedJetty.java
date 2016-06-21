package com.opentable.server;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextListener;

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
    public EmbeddedServletContainerFactory servletContainer(final Collection<ServletContextListener> listeners) {
        JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
        factory.setPort(httpBindPort);
        factory.setSessionTimeout(10, TimeUnit.MINUTES);
        factory.addInitializers(servletContext -> listeners.forEach(servletContext::addListener));
        return factory;
    }

    @Bean
    public HttpServerInfo serverInfo() {
        return new HttpServerInfo(httpBindPort);
    }
}
