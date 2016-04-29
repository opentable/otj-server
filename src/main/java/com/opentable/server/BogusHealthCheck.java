package com.opentable.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/health")
public class BogusHealthCheck {
    @GET
    public String getHealth() {
        return "Super healthy";
    }
}
