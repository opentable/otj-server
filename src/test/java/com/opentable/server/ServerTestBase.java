package com.opentable.server;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public abstract class ServerTestBase {
    private Client client;
    private ConfigurableApplicationContext context;
    private Integer port;

    @Before
    public void before() throws InterruptedException {
        Assert.assertNull(context);
        Assert.assertNull(port);
        context = SpringApplication.run(TestServer.class);
        port = context.getBeanFactory().getBean(HttpServerInfo.class).getPort();
        client = ClientBuilder.newClient();
    }

    @After
    public void after() {
        client = null;
        Assert.assertNotNull(context);
        Assert.assertNotNull(port);
        port = null;
        context.stop();
        context.close();
        context = null;
    }

    protected Response request(final String path, final String ... expectedContentType) throws IOException {
        return client.target("http://localhost:" + port).path(path).request(expectedContentType).get();
    }
}
