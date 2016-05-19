package com.opentable.server;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

// Implicitly in singleton scope, so healthy boolean persists.
@Path("/health")
public class MediocreHealthCheck {
    private static final Logger LOG = LoggerFactory.getLogger(MediocreHealthCheck.class);

    private final AtomicBoolean healthy = new AtomicBoolean();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getHealth() {
        return healthy.get() ? Response.ok("healthy").build() :
                Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("unavailable").build();
    }

    private void setHealthy(final ApplicationContextEvent event, final boolean healthy) {
        LOG.info("setting healthy {} from {}", healthy, event);
        this.healthy.set(healthy);
    }

    // Refreshed comes in as soon as everything is wired up, but closed doesn't have analogous sequencing.
    // Specifying an ordering doesn't help.  Specifying a shutdown hook still wouldn't enable us to reliably
    // get in ahead of other components.
    // TODO Improve.

    @EventListener
    public void refreshed(final ContextRefreshedEvent event) {
        setHealthy(event, true);
    }

    @EventListener
    public void closed(final ContextClosedEvent event) {
        setHealthy(event, false);
    }
}
