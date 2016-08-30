package com.opentable.server;

import java.time.Duration;

import com.google.common.annotations.VisibleForTesting;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.opentable.util.JvmFallbackShutdown;

@Component
class StartupShutdownFailedHandler {
    @VisibleForTesting
    static final Duration timeout = Duration.ofSeconds(30);

    @EventListener
    public void onFailure(ApplicationFailedEvent event) {
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }

    @EventListener
    public void onClose(ContextClosedEvent event) {
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }
}
