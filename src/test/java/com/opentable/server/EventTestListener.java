package com.opentable.server;

import javax.inject.Inject;

import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.event.EventListener;


public class EventTestListener {
    private final HttpServerInfo info;


    @Inject
    public EventTestListener(HttpServerInfo info) {
        this.info = info;
    }

    @EventListener
    public void containerInitialized(final EmbeddedServletContainerInitializedEvent evt) {
        // Should blow up if other ran first
        System.err.println("I like ports " + info.getPort());
    }
}
