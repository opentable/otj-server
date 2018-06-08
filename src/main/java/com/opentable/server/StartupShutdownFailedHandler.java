package com.opentable.server;

import java.time.Duration;

import com.google.common.annotations.VisibleForTesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.opentable.util.JvmFallbackShutdown;

@Component
public class StartupShutdownFailedHandler {
    private static final Logger LOG = LoggerFactory.getLogger(StartupShutdownFailedHandler.class);

    @VisibleForTesting
    static final Duration timeout = Duration.ofSeconds(30);

    @EventListener
    public void onFailure(ApplicationFailedEvent event) {
        LOG.debug("Received application failed event: {}", event);
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }

    @EventListener
    public void onClose(ContextClosedEvent event) {
        LOG.debug("Received context closed event: {}", event);
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }
}
