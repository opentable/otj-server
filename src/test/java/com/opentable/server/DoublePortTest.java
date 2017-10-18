package com.opentable.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.net.ServerSocket;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class DoublePortTest {
    private ConfigurableApplicationContext context;
    private Integer port;
    private int secondPort;
    private Client client;

    @Before
    public void before() throws Exception {
        client = ClientBuilder.newClient();
        try (ServerSocket ss = new ServerSocket(0)) {
            secondPort = ss.getLocalPort();
        }
        Assert.assertNull(context);
        Assert.assertNull(port);
        SpringApplication app = new SpringApplication(TestServer.class);
        app.setDefaultProperties(ImmutableMap.of(
                "ot.httpserver.active-connectors", "default-http,extra-http",
                "ot.httpserver.connector.extra-http.port", secondPort));
        context = app.run();
        port = context.getBeanFactory().getBean(HttpServerInfo.class).getPort();
    }

    @After
    public void after() {
        Assert.assertNotNull(context);
        Assert.assertNotNull(port);
        port = null;
        context.stop();
        context.close();
        context = null;
        client.close();
    }

    @Test
    public void testPorts() throws Exception {
        assertNotSame(port, secondPort);
        assertEquals(TestServer.HELLO_WORLD, readHello(port));
        assertEquals(TestServer.HELLO_WORLD, readHello(secondPort));
    }

    private String readHello(int myPort) {
        return client.target("http://localhost:" + myPort).request().get(String.class);
    }
}
