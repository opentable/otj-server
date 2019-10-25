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
package com.opentable.server.jaxrs;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.ws.rs.ext.RuntimeDelegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ListenerBootstrap;
import org.jboss.resteasy.plugins.spring.SpringBeanProcessor;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import io.smallrye.config.PropertiesConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;

@EnableConfigurationProperties
@Configuration
@Conditional(ResteasyAutoConfiguration.InstallJAXRS.class)
// Install JaxRS Server if configured to do so. Filters will still be configured for WebMVC or whatever.
public class ResteasyAutoConfiguration {
    public static class InstallJAXRS implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            final String serverType = context.getEnvironment().
                    getProperty("ot.server.type", "all");
            return "all".equals(serverType) || "jaxrs".equals(serverType);
        }
    }

    // We need access to the javax.ws.rs-api at compile scope, otherwise
    // you fail with bizarre access exceptions -- so fake out the analyzer
    static final Class<?> RUNTIME_DELEGATE = RuntimeDelegate.class;

    @Bean(name = "resteasyDispatcher")
    public FilterRegistrationBean<Filter30Dispatcher> resteasyServletRegistration(
            @Value("${ot.jaxrs.prefix:/}") String prefix) {
        if (StringUtils.isBlank(prefix)) {
            prefix = "/";
        } else {
            prefix = prefix.trim();
        }
        FilterRegistrationBean<Filter30Dispatcher> registrationBean = new FilterRegistrationBean<>(new Filter30Dispatcher());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setInitParameters(Collections.singletonMap("resteasy.servlet.mapping.prefix", prefix)); // set prefix here
        return registrationBean;
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
            servletInitParams.ifPresent(params -> params.getInitParams().forEach(servletContext::setInitParameter));
            // As of 4.0.0.CR1+ this seems to be necessary to make sure the servlet context is really config sourced.
            final Properties props = new Properties();
            servletInitParams.ifPresent(params -> params.getInitParams().forEach(props::setProperty));
            if (!props.isEmpty()) {
                final ConfigSource pconfig = new PropertiesConfigSource(props, "sourceFromServletInitParams");
                final ConfigBuilder b = new SmallRyeConfigBuilder();
                b.withSources(pconfig);
                // Copy existing sources if any
                final Iterable<ConfigSource> it = ConfigProvider.getConfig().getConfigSources();
                for (ConfigSource s : it) {
                    if ("sourceFromServletInitParams".equals(s.getName())) {
                        continue; // don't double copy
                    }
                    b.withSources(s);
                }
                ConfigProviderResolver.instance().registerConfig(b.build(), Thread.currentThread().getContextClassLoader());
            }
            final ListenerBootstrap config = new ListenerBootstrap(servletContext);
            deployment = config.createDeployment();
            deployment.start();

            servletContext.setAttribute(ResteasyProviderFactory.class.getName(), deployment.getProviderFactory());
            servletContext.setAttribute(Dispatcher.class.getName(), deployment.getDispatcher());
            servletContext.setAttribute(Registry.class.getName(), deployment.getRegistry());

            final SpringBeanProcessor processor = new SpringBeanProcessor(deployment.getDispatcher(),
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
