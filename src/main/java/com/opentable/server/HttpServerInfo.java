package com.opentable.server;

import java.util.Map;

/**
 * Expose information about the currently running HTTP(S) server.
 *
 * Servers have one or more connectors.  The default (but not always present)
 * connector is called {@code default-http}.
 */
public interface HttpServerInfo {
    /** @return the main (almost always 'http') port */
    int getPort();

    /** @return the size of the thread pool */
    int getPoolSize();

    /** @return information on the currently active server connectors */
    Map<String, ConnectorInfo> getConnectors();

    /** Expose information for a single server connector. */
    interface ConnectorInfo {
        /** @return the configuration name for this connector */
        String getName();
        /** @return the protocol the connector is expecting to speak */
        String getProtocol();
        /** @return the (actual) listen port of this connector */
        int getPort();
    }
}
