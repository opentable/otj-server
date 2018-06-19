package com.opentable.server;

public interface ServerConnectorConfig {
    default String getProtocol() {
        return "http";
    }

    default String getBindAddress() {
        return null;
    }

    default int getPort() {
        return -1;
    }

    default boolean isForceSecure() {
        return false;
    }

    default String getKeystore() {
        return null;
    }

    default String getKeystorePassword() {
        return "changeit";
    }
}
