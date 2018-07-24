/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
