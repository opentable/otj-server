package com.opentable.server;

import com.opentable.server.HttpServerInfo.ConnectorInfo;

class DefaultHttpConnectorInfo implements ConnectorInfo {
    private final EmbeddedJetty embeddedJetty;

    DefaultHttpConnectorInfo(EmbeddedJetty embeddedJetty) {
        this.embeddedJetty = embeddedJetty;
    }

    @Override
    public String getName() {
        return EmbeddedJetty.DEFAULT_CONNECTOR_NAME;
    }

    @Override
    public String getProtocol() {
        return "http";
    }

    @Override
    public int getPort() {
        return embeddedJetty.getDefaultHttpActualPort();
    }
}
