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

import java.time.Duration;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.opentable.util.JvmFallbackShutdown;

@Component
@Import(StartupShutdownFailedHandler.FallbackShutdownExitInterceptor.class)
public class StartupShutdownFailedHandler {
    private static final Logger LOG = LoggerFactory.getLogger(StartupShutdownFailedHandler.class);

    @VisibleForTesting
    static final Duration timeout = Duration.ofSeconds(30);

    @EventListener
    public void onFailure(ApplicationFailedEvent event) {
        LOG.debug("ApplicationFailedEvent {} fallback shutdown {}", event, timeout);
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }

    @EventListener
    public void onClose(ContextClosedEvent event) {
        LOG.debug("ContextClosedEvent {} fallback shutdown {}", event, timeout);
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }

    static class FallbackShutdownExitInterceptor implements ExitCodeGenerator {
        @Override
        public int getExitCode() {
            LOG.info("SpringApplication exit hook fallback shutdown {}", timeout);
            JvmFallbackShutdown.fallbackTerminate(timeout);
            return 0;
        }
    }
}
