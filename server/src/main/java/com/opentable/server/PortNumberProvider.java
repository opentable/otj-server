package com.opentable.server;

import java.io.IOException;

/**
 * Implementations of StandaloneServer may have different ways of providing port
 * information that can be injected into the portInfoProvider field.
 */
@FunctionalInterface
public interface PortNumberProvider {
    int getPort() throws IOException;
}
