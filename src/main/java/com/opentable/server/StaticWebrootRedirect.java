package com.opentable.server;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class StaticWebrootRedirect {
    private static final String INDEX_FILE_NAME = "index.html";
    private final URI location;

    public StaticWebrootRedirect() {
        this(URI.create(String.format("/%s/%s", StaticResourceConfiguration.DEFAULT_PATH_NAME, INDEX_FILE_NAME)));
    }

    @Inject
    public StaticWebrootRedirect(final StaticResourceConfiguration config) {
        this(URI.create(config.staticPath(INDEX_FILE_NAME)));
    }

    public StaticWebrootRedirect(URI location) {
        this.location = location;
    }

    @GET
    public Response redirect() {
        return Response.temporaryRedirect(location).build();
    }
}
