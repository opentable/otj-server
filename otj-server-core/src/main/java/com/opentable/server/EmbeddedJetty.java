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

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;

import com.opentable.components.filterorder.FilterOrderResolver;
import com.opentable.logging.jetty.JsonRequestLogConfig;

/**
 * Configure an embedded {@code Jetty 9} HTTP(S) server, and tie it into the Spring Boot lifecycle.
 *
 * Spring Boot provides a very basic Jetty integration but it doesn't cover a lot of important use cases.
 * For example even something as trivial as configuring the worker pool size, socket options,
 * or HTTPS connector is totally unsupported.
 * <p>
 * Customizers for web app context, namely {@code webAppContextCustomizers}, will be run from here {@link JettyServletWebServerFactory#getWebAppContextConfigurations
 * JettyServletWebServerFactory.getWebAppContextConfigurations}, more specifically subclass of {@link JettyServletWebServerFactory
 * JettyServletWebServerFactory}.
 */
@Configuration
@Import(JsonRequestLogConfig.class)
public class EmbeddedJetty extends EmbeddedJettyBase {

    @Inject
    Optional<Collection<ServletContextListener>> listeners;

    @Inject
    Optional<Collection<Consumer<WebAppContext>>> webAppContextCustomizers;


    /**
     * @param filterOrderResolver The filter order resolver is injected here since this method is the one in which we
     *                            kick off the initialization of the Jetty servlet container, which includes wiring up
     *                            the filter registration beans. We therefore want to make sure that we have resolved
     *                            the order of these filters. It is ok and expected that this variable is not used in
     *                            this method.
     * @param requestLogConfig Controls whether requests are logged via JsonRequestLog
     * @param activeConnectors Which connectors are configured. The default is just a standard http connector
     * @param pr Used to resolve the PORT settings.
     * @return ServletWebserverFactory for a factory of WebServers
     */
    @Bean
    public ServletWebServerFactory servletContainer(
            final JsonRequestLogConfig requestLogConfig,
            final Map<String, ServerConnectorConfig> activeConnectors,
            final PropertyResolver pr,
            final Optional<FilterOrderResolver> filterOrderResolver) {

        final JettyServletWebServerFactory factory = new OTJettyServletWebServerFactory(webAppContextCustomizers);
        JettyWebServerFactoryAdapter factoryAdapter = new JettyWebServerFactoryAdapter(factory);
        this.configureFactoryContainer(requestLogConfig, activeConnectors, pr, factoryAdapter);

        if (factory.getSession() != null) {
            factory.getSession().setTimeout(Duration.ofMinutes(10));
        }
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
            if (factory.getSession() != null) {
                factory.getSession().setTimeout(duration);
            }
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
