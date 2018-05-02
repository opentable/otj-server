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
        LOG.info("ApplicationFailedEvent {} fallback shutdown {}", event, timeout);
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }

    @EventListener
    public void onClose(ContextClosedEvent event) {
        LOG.info("ContextClosedEvent {} fallback shutdown {}", event, timeout);
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
