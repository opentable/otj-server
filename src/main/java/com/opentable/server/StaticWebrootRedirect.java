package com.opentable.server;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/")
public class StaticWebrootRedirect {
    private final URI location;

    public StaticWebrootRedirect() {
        this(URI.create("/static/index.html"));
    }

    public StaticWebrootRedirect(URI location) {
        this.location = location;
    }

    @GET
    public Response redirect() {
        return Response.temporaryRedirect(location).build();
    }
}
