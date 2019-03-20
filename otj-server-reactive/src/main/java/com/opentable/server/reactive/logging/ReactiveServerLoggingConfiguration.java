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
package com.opentable.server.reactive.logging;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.JsonRequestLogConfig;

/**
 * Configures automatic server request logging using Jetty for reactive applications.
 *
 * @deprecated Reactive services should utiliz the JsonRequestLog that is already imported as part of the
 * EmbeddedJetty server.
 */
@Deprecated
@Configuration
public class ReactiveServerLoggingConfiguration {

    /**
     * Override the bean injected in to EmbeddedJettyBase with our own configuration.
     */
    @Bean
    public JsonRequestLog requestLogger(JsonRequestLogConfig config) {
        return new ServerRequestLog(Clock.systemUTC(), config);
    }
}
