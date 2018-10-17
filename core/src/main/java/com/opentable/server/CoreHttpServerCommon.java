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

import com.opentable.metrics.http.HealthHttpConfiguration;
import com.opentable.metrics.http.MetricsHttpConfiguration;

/**
 * Common configuration for REST HTTP Server instances
 * Public so "non web users" can access.
 * If you need a http client, see the README
 *
 * @see ServerLoggingConfiguration for its special setup.
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
    // Filter for transfer core info to MDC
    BackendInfoFilterConfiguration.class,
    // Pull configuration info for server connector
    ServerConfigConfiguration.class,
    // Support static resources
    StaticResourceConfiguration.class,
    // Default health check
    HealthHttpConfiguration.class,
    // Metrics for http
    MetricsHttpConfiguration.class,
})
// All the non web stuff
@NonWebSetup
@interface CoreHttpServerCommon {
}
