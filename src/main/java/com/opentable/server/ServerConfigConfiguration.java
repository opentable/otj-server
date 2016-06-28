package com.opentable.server;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import com.opentable.service.AppInfo;
import com.opentable.service.EnvInfo;
import com.opentable.spring.ConversionServiceConfiguration;

@Configuration
@Import({
    ConversionServiceConfiguration.class,
    AppInfo.class,
    EnvInfo.class,
})
public class ServerConfigConfiguration {
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfig() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Inject
    public void logAppConfig(final ConfigurableEnvironment env) {
        final Logger log = LoggerFactory.getLogger(ServerConfigConfiguration.class);
        log.info(env.toString());
    }
}
