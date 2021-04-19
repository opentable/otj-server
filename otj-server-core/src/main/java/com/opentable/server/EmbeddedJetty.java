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
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.web.embedded.jetty.ConfigurableJettyWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertyResolver;

import com.opentable.components.filterorder.FilterOrderResolver;
import com.opentable.logging.jetty.JsonRequestLogConfig;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ObjectUtils;

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
@Import({JsonRequestLogConfig.class, EmbeddedJetty.BeanPostProcessorsRegistrar.class})
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
     *
     */
    @Bean
    public ServletWebServerFactory servletContainer(final Optional<FilterOrderResolver> filterOrderResolver) {
        return new OTJettyServletWebServerFactory(webAppContextCustomizers, showStacks);
    }

    /**
     * @param requestLogConfig Controls whether requests are logged via JsonRequestLog
     * @param activeConnectors Which connectors are configured. The default is just a standard http connector
     * @param pr Used to resolve the PORT settings.
     * @return ServletWebserverFactory for a factory of WebServers
     */
    @Bean
    @Order(1)
    public WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory> otJettyWebServerFactoryWebServerFactoryCustomizer(
            final JsonRequestLogConfig requestLogConfig,
            final Map<String, ServerConnectorConfig> activeConnectors,
            final PropertyResolver pr
    ) {
        return configurableJettyWebServerFactory -> {
            JettyServletWebServerFactory factory = (JettyServletWebServerFactory) configurableJettyWebServerFactory;
            JettyWebServerFactoryAdapter factoryAdapter = new JettyWebServerFactoryAdapter(factory);
            this.configureFactoryContainer(requestLogConfig, activeConnectors, pr, factoryAdapter);
            if (factory.getSession() != null) {
                factory.getSession().setTimeout(Duration.ofMinutes(10));
            }
            if (listeners.isPresent()) {
                factory.addInitializers(servletContext -> listeners.get().forEach(servletContext::addListener));
            }
        };
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

    public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

        private ConfigurableListableBeanFactory beanFactory;

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            if (beanFactory instanceof ConfigurableListableBeanFactory) {
                this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
            }
        }

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                            BeanDefinitionRegistry registry) {
            if (this.beanFactory == null) {
                return;
            }
            registerSyntheticBeanIfMissing(registry, "webServerFactoryCustomizerBeanPostProcessor",
                    WebServerFactoryCustomizerBeanPostProcessor.class,
                    WebServerFactoryCustomizerBeanPostProcessor::new);
            registerSyntheticBeanIfMissing(registry, "errorPageRegistrarBeanPostProcessor",
                    ErrorPageRegistrarBeanPostProcessor.class, ErrorPageRegistrarBeanPostProcessor::new);
        }

        private <T> void registerSyntheticBeanIfMissing(BeanDefinitionRegistry registry, String name,
                                                        Class<T> beanClass, Supplier<T> instanceSupplier) {
            if (ObjectUtils.isEmpty(this.beanFactory.getBeanNamesForType(beanClass, true, false))) {
                RootBeanDefinition beanDefinition = new RootBeanDefinition(beanClass, instanceSupplier);
                beanDefinition.setSynthetic(true);
                registry.registerBeanDefinition(name, beanDefinition);
            }
        }

    }

}
