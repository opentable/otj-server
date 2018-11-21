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
package com.opentable.server.jaxrs;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;

import com.opentable.jaxrs.JaxRsClientConfiguration;

// Install JaxRS client and filters only if configured to do so
@Configuration
@Conditional(ResteasyAutoConfiguration.InstallJAXRS.class)
@Import(JaxRsClientConfiguration.class)
public class JaxRSClientShimConfiguration {
    public static class InstallJAXRS implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            // Only if both or specifically jaxrs
            final String clientType = context.getEnvironment().
                    getProperty("ot.client.type", "all");
            return "all".equals(clientType) || "jaxrs".equals(clientType);
        }
    }
}
