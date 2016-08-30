package com.opentable.server;

public class HttpServerInfo {
    private final int port;

    HttpServerInfo(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
