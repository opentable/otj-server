package com.opentable.server;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource
public class JettyDumper {

    private final Provider<Server> jetty;

    @Inject
    JettyDumper(Provider<Server> jetty) {
        this.jetty = jetty;
    }

    @ManagedOperation
    public String dumpJetty() throws IOException {
        final StringBuilder dump = new StringBuilder();
        jetty.get().dump(dump, "  ");
        final String result = dump.toString();
        LoggerFactory.getLogger(JettyDumper.class).info("Jetty Internal State\n{}", result);
        return result;
    }
}
