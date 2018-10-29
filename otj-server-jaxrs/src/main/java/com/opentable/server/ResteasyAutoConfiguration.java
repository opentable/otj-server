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

import java.util.Optional;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.ext.RuntimeDelegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ListenerBootstrap;
import org.jboss.resteasy.plugins.spring.SpringBeanProcessor;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.springmvc.ResteasyHandlerAdapter;
import org.jboss.resteasy.springmvc.ResteasyHandlerMapping;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@EnableConfigurationProperties
@Configuration
// Install ResteasyHandlerMapping (http://docs.jboss.org/resteasy/docs/1.0.1.GA/userguide/html/RESTEasy_Spring_Integration.html)
public class ResteasyAutoConfiguration {

    // We need access to the javax.ws.rs-api at compile scope, otherwise
    // you fail with bizarre access exceptions -- so fake out the analyzer
    static final Class<?> RUNTIME_DELEGATE = RuntimeDelegate.class;

    @Bean
    @Inject
    public ResteasyHandlerMapping resteasyHandlerMapping(RestEasySpringInitializer initializer) {
        ResteasyHandlerMapping res = new ResteasyHandlerMapping(initializer.deployment);
        res.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return res;
    }

    @Bean
    @Inject
    public ResteasyHandlerAdapter resteasyHandlerAdapter(RestEasySpringInitializer initializer) {
        return new ResteasyHandlerAdapter(initializer.deployment);
    }

    @Bean(destroyMethod = "cleanup")
    @Inject
    public static RestEasySpringInitializer restEasySpringInitializer(Optional<ServletInitParameters> servletInitParams) {
        return new RestEasySpringInitializer(servletInitParams);
    }

    @Bean
    public JacksonJsonProvider jacksonJsonProvider(ObjectMapper mapper) {
        return new JacksonJsonProvider(mapper);
    }

    @Bean
    public OTCorsFilter corsFilter() {
        return new OTCorsFilter();
    }

    public static class RestEasySpringInitializer
            implements
                ServletContextInitializer,
                ApplicationContextAware,
                BeanFactoryPostProcessor {

        private ResteasyDeployment deployment;

        private ConfigurableApplicationContext applicationContext;

        private ConfigurableListableBeanFactory beanFactory;

        private final Optional<ServletInitParameters> servletInitParams;

        public RestEasySpringInitializer(Optional<ServletInitParameters> servletInitParams) {
            this.servletInitParams = servletInitParams;
        }

        public void cleanup() {
            if (deployment != null) {
                deployment.stop();
                deployment = null;
            }
        }

        @Override
        public void onStartup(ServletContext servletContext) throws ServletException {
            servletInitParams.ifPresent(params -> params.getInitParams().forEach((key, value) -> servletContext.setInitParameter(key, value)));

            ListenerBootstrap config = new ListenerBootstrap(servletContext);
            deployment = config.createDeployment();
            deployment.start();

            servletContext.setAttribute(ResteasyProviderFactory.class.getName(), deployment.getProviderFactory());
            servletContext.setAttribute(Dispatcher.class.getName(), deployment.getDispatcher());
            servletContext.setAttribute(Registry.class.getName(), deployment.getRegistry());

            SpringBeanProcessor processor = new SpringBeanProcessor(deployment.getDispatcher(),
                    deployment.getRegistry(), deployment.getProviderFactory());
            processor.postProcessBeanFactory(beanFactory);
            applicationContext.addApplicationListener(processor);
        }

        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = (ConfigurableApplicationContext) applicationContext;
        }
    }
}
