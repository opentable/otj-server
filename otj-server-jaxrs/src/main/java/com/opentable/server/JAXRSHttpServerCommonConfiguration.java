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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.metrics.jaxrs.HealthHttpJaxRsConfiguration;
import com.opentable.metrics.jaxrs.MetricsHttpJaxRsConfiguration;
import com.opentable.metrics.jaxrs.ReadyHttpJaxRsConfiguration;
import com.opentable.server.jaxrs.ConservedHeadersConfiguration;
import com.opentable.server.jaxrs.FilterOrderConfiguration;
import com.opentable.server.jaxrs.JaxRSClientShimConfiguration;
import com.opentable.server.jaxrs.ResteasyAutoConfiguration;
import com.opentable.servicesecurity.servlet.ServiceSecurityFilterConfiguration;

/**
 * Common configuration for REST HTTP Server instances
 *
 * @see ServerLoggingConfiguration for its special setup.
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        // Core resteasy dispatcher, jackson, etc
        ResteasyAutoConfiguration.class,
        // JaxRS wiring
        JaxRSClientShimConfiguration.class,
        // Forces jaxrs servlet to go last
        FilterOrderConfiguration.class,
        // Default health check
        HealthHttpJaxRsConfiguration.class,
        // Default Ready Check
        ReadyHttpJaxRsConfiguration.class,
        // Metrics for http
        MetricsHttpJaxRsConfiguration.class,
        // Conserved Headers
        ConservedHeadersConfiguration.class,
        // Filter for applying service security verification on incoming requests
        ServiceSecurityFilterConfiguration.class,
})
@interface JAXRSHttpServerCommonConfiguration {
}
