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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import com.opentable.spring.SpecializedConfigFactory;

/**
 * Provide beans required to construct implementations of {@link EmbeddedJettyBase}.
 */
@Configuration
public class EmbeddedJettyConfiguration {

    @Bean
    public SpecializedConfigFactory<ServerConnectorConfig> connectorConfigs(ConfigurableEnvironment environment) {
        return SpecializedConfigFactory.create(environment, ServerConnectorConfig.class, "ot.httpserver.connector.${name}");
    }
}
