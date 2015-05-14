package com.opentable.server;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;

class StandaloneServerHealth extends HealthCheck {

    private final StandaloneServer server;

    @Inject
    StandaloneServerHealth(StandaloneServer server) {
        this.server = server;
    }

    @Override
    protected Result check() throws Exception {
        if (server.isStopped()) {
            return Result.unhealthy("Server stopped");
        }
        if (!server.isStarted()) {
            return Result.unhealthy("Server still starting up");
        }
        return Result.healthy();
    }
}
