package com.opentable.server;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.web.servlet.HandlerMapping;

import com.opentable.jackson.OpenTableJacksonConfiguration;
import com.opentable.metrics.http.MVCMetricsEndpointConfiguration;

@Configuration
@EnableConfigurationProperties
@Import({
    // Core dispatcher for MVC servlets
    ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
    DispatcherServletAutoConfiguration.class,
    // Core MVC
    WebMvcAutoConfiguration.class,
    // Converts JSON/XML using jackson
    JacksonAutoConfiguration.class,
    HttpMessageConvertersAutoConfiguration.class,
    // Error handler default (I'm ambivalent about this - will go with consensus
    ErrorMvcAutoConfiguration.class,
    // To add dependency
    PropertyPlaceholderAutoConfiguration.class,
    // Redundant but prevents wiring warnings in IDE
    OpenTableJacksonConfiguration.class,
    MVCMetricsEndpointConfiguration.class,
    ConservedHeadersConfiguration.class,
})
class MVCHttpServerCommonConfiguration {

    // To make dependency checker happy.
    // We want spring-webmvc to be transitive here.
    public static String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

    @Inject
    MVCHttpServerCommonConfiguration(ObjectMapper objectMapper, HttpMessageConverters httpMessageConverters) {
        setupConverter(httpMessageConverters.getConverters(), objectMapper);
    }

    private void setupConverter(final List<HttpMessageConverter<?>> converterList, ObjectMapper objectMapper) {
        converterList.stream()
            .filter(t -> t instanceof AbstractJackson2HttpMessageConverter)
            .map(t -> (AbstractJackson2HttpMessageConverter) t)
            .forEach(converter -> converter.setObjectMapper(objectMapper));
    }
}
