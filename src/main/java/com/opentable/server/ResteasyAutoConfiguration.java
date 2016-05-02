package com.opentable.server;

import java.util.Collections;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.ext.RuntimeDelegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.jboss.resteasy.plugins.server.servlet.ListenerBootstrap;
import org.jboss.resteasy.plugins.spring.SpringBeanProcessor;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties
@Configuration
public class ResteasyAutoConfiguration {

    // We need access to the javax.ws.rs-api at compile scope, otherwise
    // you fail with bizarre access exceptions -- so fake out the analyzer
    static final Class<?> RUNTIME_DELEGATE = RuntimeDelegate.class;

    @Bean(name = "resteasyDispatcher")
    public FilterRegistrationBean resteasyServletRegistration() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(new FilterDispatcher());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setInitParameters(Collections.singletonMap("resteasy.servlet.mapping.prefix", "/")); // set prefix here
        return registrationBean;
    }

    @Bean(destroyMethod = "cleanup")
    public static RestEasySpringInitializer restEasySpringInitializer() {
        return new RestEasySpringInitializer();
    }

    @Bean
    public JacksonJsonProvider jacksonJsonProvider(ObjectMapper mapper) {
        return new JacksonJsonProvider(mapper);
    }

    public static class RestEasySpringInitializer
            implements
                ServletContextInitializer,
                ApplicationContextAware,
                BeanFactoryPostProcessor {

        private ResteasyDeployment deployment;

        private ConfigurableApplicationContext applicationContext;

        private ConfigurableListableBeanFactory beanFactory;

        public void cleanup() {
            if (deployment != null) {
                deployment.stop();
                deployment = null;
            }
        }

        @Override
        public void onStartup(ServletContext servletContext) throws ServletException {
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
