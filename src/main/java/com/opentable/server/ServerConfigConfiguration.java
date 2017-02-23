package com.opentable.server;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.ConfigurableEnvironment;

import com.opentable.service.AppInfo;
import com.opentable.service.EnvInfo;
import com.opentable.spring.ConversionServiceConfiguration;
import com.opentable.spring.PropertySourceUtil;

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
        log.info("{}", env);
        PropertySourceUtil.getProperties(env)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(), // Key mapper.
                        Map.Entry::getValue,        // Value mapper.
                        (p1, p2) -> {
                            log.warn("duplicate resolved properties; picking first: {}, {}", p1, p2);
                            return p1;
                        }
                ))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> log.info("{}: {}", e.getKey(), e.getValue()));
    }
}
