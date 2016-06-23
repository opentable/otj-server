package com.opentable.server;

import java.io.IOException;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.service.ServiceInfo;

public class RestHttpServerTest {
    private ConfigurableApplicationContext context;
    private Integer port;

    @Before
    public void before() throws InterruptedException {
        Assert.assertNull(context);
        Assert.assertNull(port);
        context = SpringApplication.run(TestServer.class);
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
    }

    @Test
    public void testHello() throws IOException {
        Assert.assertEquals(TestServer.HELLO_WORLD, readString("/", null));
    }

    @Test
    public void testMissing() throws IOException {
        try (final CloseableHttpResponse resp = getResponse("/not/found/omg/wtf/bbq")) {
            Assert.assertEquals(404, resp.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testStatic_txt() throws IOException {
        testStatic("static/test.txt", "text/plain");
    }

    @Test
    public void testStatic_png() throws IOException {
        testStatic("static/test.png", "image/png");
    }

    private CloseableHttpResponse getResponse(final String path) throws IOException {
        final CloseableHttpClient client = HttpClients.createMinimal();
        final HttpGet get = new HttpGet("http://localhost:" + port + path);
        return client.execute(get);
    }

    private byte[] readBytes(final String path, final String expectedContentType) throws IOException {
        try (final CloseableHttpResponse resp = getResponse(path)) {
            Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
            if (expectedContentType != null) {
                Assert.assertEquals(expectedContentType, resp.getFirstHeader("Content-Type").getValue());
            }
            return IOUtils.toByteArray(resp.getEntity().getContent());
        }
    }

    private String readString(final String path, final String expectedContentType) throws IOException {
        return new String(readBytes(path, expectedContentType));
    }

    private void testStatic(final String path, final String expectedContentType) throws IOException {
        final byte[] expected = IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream(path));
        final byte[] actual = readBytes("/" + path, expectedContentType);
        Assert.assertArrayEquals(expected, actual);
    }
}
