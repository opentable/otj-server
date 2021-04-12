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

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Determines whether the BackendInfo filters are wired up. Those expose useful, but
 * security dubious information in the HTTP response.
 */
public class WireBackendInfo implements Condition {

    public static final String OT_SERVER_BACKEND_INFO_ENABLED = "ot.server.backend.info.enabled";
    private static final String DEFAULT_VALUE = "true";

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        return Boolean.parseBoolean(conditionContext.getEnvironment().getProperty(OT_SERVER_BACKEND_INFO_ENABLED, DEFAULT_VALUE));
    }
}
