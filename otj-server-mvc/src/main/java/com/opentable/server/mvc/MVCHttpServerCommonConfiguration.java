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
package com.opentable.server.mvc;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.HandlerMapping;

import com.opentable.conservedheaders.CoreConservedHeadersConfiguration;
import com.opentable.jackson.OpenTableJacksonConfiguration;
import com.opentable.metrics.mvc.HealthHttpMVCConfiguration;
import com.opentable.metrics.mvc.MetricsHttpMVCConfiguration;
import com.opentable.metrics.mvc.ReadyHttpMVCConfiguration;
import com.opentable.server.ThreadNameFilterConfiguration;
import com.opentable.servicesecurity.servlet.ServiceSecurityFilterConfiguration;

@Configuration
@EnableConfigurationProperties
@Import({
        // Core dispatcher for MVC servlets
        ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        DispatcherServletAutoConfiguration.class,
        // Core MVC
        WebMvcAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        // Error handler default (I'm ambivalent about this - will go with consensus
        ErrorMvcAutoConfiguration.class,
        // Redundant but prevents wiring warnings in IDE
        OpenTableJacksonConfiguration.class,
        HealthHttpMVCConfiguration.class,
        MetricsHttpMVCConfiguration.class,
        ReadyHttpMVCConfiguration.class,
        CoreConservedHeadersConfiguration.class,
        // Logging exception handler
        LoggingHandlerExceptionResolver.class,
        ThreadNameFilterConfiguration.class,
        // Filter for applying service security verification on incoming requests
        ServiceSecurityFilterConfiguration.class,
})
class MVCHttpServerCommonConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MVCHttpServerCommonConfiguration.class);


    // To make dependency checker happy.
    // We want spring-webmvc to be transitive here.
    public static final String URI_TEMPLATE_VARIABLES_ATTRIBUTE = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

    @Inject
    MVCHttpServerCommonConfiguration(ObjectMapper objectMapper, HttpMessageConverters httpMessageConverters) {
        setupConverter(httpMessageConverters.getConverters(), objectMapper, MappingJackson2HttpMessageConverter.class);
    }

    private <T extends AbstractJackson2HttpMessageConverter> void setupConverter(final List<HttpMessageConverter<?>> converterList, ObjectMapper objectMapper, final Class<T> converterClazz) {
        converterList.stream()
            .filter(converterClazz::isInstance)
            .map(t -> (AbstractJackson2HttpMessageConverter) t)
            .forEach(converter -> {
                try {
                    if (objectMapper != converter.getObjectMapper()) {
                        converter.setObjectMapper(objectMapper);
                        log.debug("Converter {} is configured", converter);
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Error configuring converter {}: {}", converter, e.getMessage());
                }
            });
    }

}
