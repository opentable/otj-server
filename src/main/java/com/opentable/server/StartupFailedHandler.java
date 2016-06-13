package com.opentable.server;

import java.time.Duration;

import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

import com.opentable.util.JvmFallbackShutdown;

class StartupFailedHandler implements ApplicationListener<ApplicationFailedEvent> {
    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        JvmFallbackShutdown.fallbackTerminate(Duration.ofSeconds(5));
    }
}
