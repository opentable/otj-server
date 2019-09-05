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
package com.opentable.server.reactive;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.conservedheaders.reactive.ReactiveServerConservedHeadersConfiguration;
import com.opentable.metrics.reactive.HealthHttpReactiveConfiguration;
import com.opentable.metrics.reactive.MetricsHttpReactiveConfiguration;
import com.opentable.metrics.reactive.ReadyHttpReactiveConfiguration;
import com.opentable.server.EmbeddedJettyConfiguration;
import com.opentable.server.EmbeddedReactiveJetty;
import com.opentable.server.NonWebSetup;
import com.opentable.server.reactive.webfilter.BackendInfoWebFilterConfiguration;
import com.opentable.servicesecurity.reactive.ServiceSecurityWebFilterConfiguration;

/**
 * Common configuration for Spring WebFlux reactive servers.
 */
@Configuration
@EnableConfigurationProperties
@Import({
        // OT Embedded Jetty
        EmbeddedJettyConfiguration.class,
        EmbeddedReactiveJetty.class,
        // Conserved headers
        ReactiveServerConservedHeadersConfiguration.class,
        // Default health check
        HealthHttpReactiveConfiguration.class,
        // Ready check
        ReadyHttpReactiveConfiguration.class,
        // Metrics for http
        MetricsHttpReactiveConfiguration.class,
        // Filter for transfer of core backend info
        BackendInfoWebFilterConfiguration.class,
        // Filter for applying service security verification on incoming requests
        ServiceSecurityWebFilterConfiguration.class,

        // Support static resources
        // TODO: Need to test serving static resources the WebFlux way. See OTPL-3648.
})
@ImportAutoConfiguration({
        // Core WebFlux
        ReactiveWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        ErrorWebFluxAutoConfiguration.class,
        WebFluxAutoConfiguration.class,
        HttpHandlerAutoConfiguration.class,
})
@NonWebSetup
class ReactiveServerCommonConfiguration {
}
