package com.opentable.server;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;

/**
 * Mark requests as secure independent of the transport we got them on.
 * Mostly useful when you are handling SSL traffic but an external
 * process terminated SSL for you.
 */
class SuperSecureCustomizer extends SecureRequestCustomizer {
    @Override
    public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
        super.customize(connector, channelConfig, request);
        if (!request.isSecure()) {
            super.customizeSecure(request);
        }
    }
}
