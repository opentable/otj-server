package com.opentable.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.opentable.server.ServerConfigConfiguration.LogAppConfig;
import com.opentable.spring.ConversionServiceConfiguration;

@Configuration
@Import({
    ConversionServiceConfiguration.class,
    LogAppConfig.class
})
public class ServerConfigConfiguration {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfig() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Component
    static class LogAppConfig implements EnvironmentAware {
        @Override
        public void setEnvironment(Environment environment) {
            Logger log = LoggerFactory.getLogger(LogAppConfig.class);
            ((ConfigurableEnvironment) environment).getPropertySources().forEach(ps -> {
                log.debug("Application uses property source {}", ps);
            });
        }
    }
}
