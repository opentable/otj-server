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

import com.opentable.jackson.OpenTableJacksonConfiguration;
import com.opentable.metrics.DefaultMetricsConfiguration;
import com.opentable.pausedetector.EnablePauseDetector;

/**
 * Common configuration for REST HTTP Server instances
 * Public so "non web users" can access.
 * If you need a http client, see the README
 *
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
    // Customized object mapper
    OpenTableJacksonConfiguration.class,
    // Setup JMX server
    JmxConfiguration.class,
    // Setup metrics
    DefaultMetricsConfiguration.class,
    StartupShutdownFailedHandler.class,
    // Spit out version info on startup
    PreFlight.class,
    // Hook up Jetty Dump as MBEAN operation
    JettyDumper.class,
    // Sets up app and environment info beans and property converters
    ServerConfigConfiguration.class,
})
@EnablePauseDetector
public @interface NonWebSetup {
}
