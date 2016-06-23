package com.opentable.server;

import java.time.Duration;

import com.google.common.annotations.VisibleForTesting;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

import com.opentable.util.JvmFallbackShutdown;

class StartupFailedHandler implements ApplicationListener<ApplicationFailedEvent> {
    @VisibleForTesting
    static final Duration timeout = Duration.ofSeconds(5);

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        JvmFallbackShutdown.fallbackTerminate(timeout);
    }
}
