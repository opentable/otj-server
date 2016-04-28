package com.opentable.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.format.support.FormattingConversionService;

@Configuration
public class ServerConfigConfiguration {
    @Bean
    public static FormattingConversionService conversionService() {
        final FormattingConversionService reg = new DefaultFormattingConversionService();
        new DateTimeFormatterRegistrar().registerFormatters(reg);
        return reg;
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyConfigInDev(FormatterRegistry reg) {
        return new PropertySourcesPlaceholderConfigurer();
    }
}
