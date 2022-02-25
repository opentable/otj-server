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

import java.util.function.Consumer;

import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.lang.NonNull;

@Configuration
@Conditional(EmbeddedJettyConnectionLimit.InstallEmbeddedJettyConnectionLimit.class)
public class EmbeddedJettyConnectionLimit {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedJettyConnectionLimit.class);

    public static class InstallEmbeddedJettyConnectionLimit implements Condition {
        @Override
        public boolean matches(ConditionContext context, @NonNull AnnotatedTypeMetadata metadata) {
            final String value = context.getEnvironment().
                    getProperty("ot.server.connection-limit.enabled", "false");
            return Boolean.parseBoolean(value);
        }
    }

    /**
     * connectionLimit - number of simultaneous connections to permit, before new connection will stop being accepted.
     * Ignored if the value is less than or equal to 0, or if ot.server.connection-limit.enabled is not set to true
     */
    private final Integer connectionLimit;

    public EmbeddedJettyConnectionLimit( @Value("${ot.server.connection-limit:500}") Integer connectionLimit) {
        this.connectionLimit = connectionLimit;
    }
    ConnectionLimit connectionLimit(int limit, Server server) {
        LOG.debug("Installing ConnectionLimit {}", limit);
        return new ConnectionLimit(limit, server);
    }

    @Bean
    public Consumer<Server> lowResourcesCustomizer() {
        return  server -> {
           if (connectionLimit > 0) {
               server.addBean(connectionLimit(connectionLimit, server));
           }
        };
    }

}
