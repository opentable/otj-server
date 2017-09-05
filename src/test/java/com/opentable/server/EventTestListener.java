package com.opentable.server;

import java.util.concurrent.atomic.AtomicBoolean;


import javax.inject.Inject;

import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.context.event.EventListener;


public class EventTestListener {
    private final EmbeddedJetty jetty;
    private final AtomicBoolean success = new AtomicBoolean();
    private final AtomicBoolean initialized = new AtomicBoolean();

    @Inject
    public EventTestListener(EmbeddedJetty embeddedJetty) {
        this.jetty = embeddedJetty;
    }

    @EventListener
    public void containerInitialized(final EmbeddedServletContainerInitializedEvent evt) {
        // This can only set success to true IF and only IF Atomically the other event ran first
        // (assuming synchronicity)
        success.set(jetty.getContainerInitialized().compareAndSet(true, false));
        announceInitialized();
    }

    public void waitForInitialized() {
        synchronized (initialized) {
            while (!initialized.get()) {
                try {
                    initialized.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void announceInitialized() {
        synchronized (initialized) {
            initialized.set(true);
            initialized.notifyAll();
        }
    }

    public boolean getSuccess() {
        return success.get();
    }
}
