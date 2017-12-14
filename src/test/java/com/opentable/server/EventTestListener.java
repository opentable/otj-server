package com.opentable.server;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;


public class EventTestListener {
    private static final Logger LOG = LoggerFactory.getLogger(EventTestListener.class);
    private final HttpServerInfo info;


    @Inject
    public EventTestListener(HttpServerInfo info) {
        this.info = info;
    }

    @EventListener
    public void containerInitialized(final WebServerInitializedEvent evt) {
        // Should blow up if other didn't first
        LOG.debug("I like ports " + info.getPort());
    }
}
