package com.opentable.server;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

@Named
class LoopbackRequest {
    private final Client client;
    private final Provider<HttpServerInfo> info;
    @Inject
    private LoopbackRequest(@Named("test") Client client, Provider<HttpServerInfo> info) {
        this.client = client;
        this.info = info;
    }

    public WebTarget of(String path) {
        return client.target("http://localhost:" + info.get().getPort()).path(path);
    }
}
