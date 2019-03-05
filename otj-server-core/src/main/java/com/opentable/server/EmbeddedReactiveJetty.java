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
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.JettyHttpHandlerAdapter;

import com.opentable.logging.jetty.JsonRequestLogConfig;

/**
 * Configure an embedded {@code Jetty 9} HTTP(S) server for use with reactive spring, and tie it into the Spring Boot lifecycle.
 *
 * Spring Boot provides a very basic Jetty integration but it doesn't cover a lot of important use cases.
 * For example even something as trivial as configuring the worker pool size, socket options,
 * or HTTPS connector is totally unsupported.
 */
@Configuration
@Import(JsonRequestLogConfig.class)
public class EmbeddedReactiveJetty extends EmbeddedJettyBase {

    @Bean
    public JettyReactiveWebServerFactory webServerFactory(final JsonRequestLogConfig requestLogConfig,
                                                          final Map<String, ServerConnectorConfig> activeConnectors,
                                                          final PropertyResolver pr) {
        final JettyReactiveWebServerFactory factory = new JettyReactiveWebServerFactory() {
            @Override
            public WebServer getWebServer(HttpHandler httpHandler) {
                JettyHttpHandlerAdapter servlet = new JettyHttpHandlerAdapter(httpHandler);
                Server server = createJettyServer(servlet);
                return new JettyWebServer(server, true);
            }
        };
        JettyReactiveWebServerFactoryAdapter factoryAdapter = new JettyReactiveWebServerFactoryAdapter(factory);
        this.configureFactoryContainer(requestLogConfig, activeConnectors, pr, factoryAdapter);
        return factoryAdapter.getFactory();

    }

    static class JettyReactiveWebServerFactoryAdapter implements WebServerFactoryAdapter<JettyReactiveWebServerFactory> {
        private final JettyReactiveWebServerFactory factory;

        JettyReactiveWebServerFactoryAdapter(JettyReactiveWebServerFactory factory) {
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
            // Currently Spring Boot 2.1.2 does not expose this property at this level. Possibly an oversight
            // Also it is questionable that anyone is actually using this feature
            // consider removing
            throw new UnsupportedOperationException("operation not supported by JettyReactiveWebServerFactory");
        }

        @Override
        public void setThreadPool(ThreadPool threadPool) {
            factory.setThreadPool(threadPool);
        }

        // TODO: Investigate if this is supported now
        @Override
        public void addInitializers(ServletContextInitializer... initializers) {
            // currently not supported by Spring Boot 2.1.2. Consider removing since we only
            // add two listeners health and metrics which have spring equivalents
            throw new UnsupportedOperationException("operation not supported by JettyReactiveWebServerFactory");
        }

        @Override
        public JettyReactiveWebServerFactory getFactory() {
            return factory;
        }
    }

}
