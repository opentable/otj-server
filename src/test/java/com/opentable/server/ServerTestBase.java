package com.opentable.server;

import java.util.HashMap;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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

    protected RequestBuilder request(final String path, final String expectedContentType) {
        return new RequestBuilder(path, expectedContentType);
    }

    protected RequestBuilder request(final String path) {
        return request(path, null);
    }

    /**
     * Add builder methods as needed.
     */
    class RequestBuilder {
        private final String expectedContentType;
        private WebTarget target;
        private final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private RequestBuilder(final String path, final String expectedContentType) {
            target = client.target("http://localhost:" + port).path(path);
            this.expectedContentType = expectedContentType;
        }
        RequestBuilder header(final String name, final Object value) {
            headers.add(name, value);
            return this;
        }
        Response get() {
            final Invocation.Builder ib = target.request();
            ib.headers(headers);
            final Response resp = ib.get();
            if (expectedContentType != null) {
                Assert.assertEquals(expectedContentType, resp.getHeaderString("Content-Type"));
            }
            return resp;
        }
    }
}
