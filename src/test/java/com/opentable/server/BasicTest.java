package com.opentable.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.codahale.metrics.Counting;
import com.codahale.metrics.MetricRegistry;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class
})
@TestPropertySource(properties= {
    "ot.httpserver.max-threads=13",
    "ot.httpserver.static-path=static-test",
})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class BasicTest {
    @Inject
    LoopbackRequest request;

    @Inject
    EmbeddedJetty ej;

    @Inject
    MetricRegistry metrics;

    @Test(timeout = 10_000)
    public void test5xx() throws IOException, InterruptedException {
        String responseText = request.of("/5xx").request().get().readEntity(String.class);
        assertEquals(TestServer.TestErrorHandler.TEXT, responseText);
        waitForCount("http-server.500-responses", 1);
    }

    @Test(timeout = 10_000)
    public void testHello() throws IOException, InterruptedException {
        assertEquals(TestServer.HELLO_WORLD, request.of("/").request().get().readEntity(String.class));
        waitForCount("http-server.200-responses", 1);
    }

    @Test(timeout = 10_000)
    public void testMissing() throws IOException, InterruptedException {
        Response r = request.of("/not/found/omg/wtf/bbq").request().get();
        assertEquals(404, r.getStatus());
        waitForCount("http-server.404-responses", 1);
    }

    @Test(timeout = 10_000)
    public void testAccessDenied() throws InterruptedException {
        Response r = request.of("/nuclear-launch-codes").request().get();
        assertEquals(403, r.getStatus());
        waitForCount("http-server.403-responses", 1);
    }

    @Test
    public void testStatic_txt() throws IOException {
        testStatic("static-test/test.txt", "text/plain");
    }

    @Test
    public void testStatic_png() throws IOException {
        testStatic("static-test/test.png", "image/png");
    }

    @Test
    public void testPoolCustomizations() throws Exception {
        QueuedThreadPool qtp = ej.getThreadPool();
        assertEquals(13, qtp.getMinThreads());
        assertEquals(13, qtp.getMaxThreads());
    }

    private void testStatic(final String path, final String expectedContentType) throws IOException {
        final byte[] expected = IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream(path));
        final Response r = request.of(path).request(expectedContentType).get();
        assertEquals(expectedContentType, r.getHeaderString(HttpHeaders.CONTENT_TYPE));
        final byte[] actual = r.readEntity(byte[].class);
        Assert.assertArrayEquals(expected, actual);
    }

    private void waitForCount(final String metricName, final long expected) throws InterruptedException {
        while (true) {
            final Counting c = (Counting) metrics.getMetrics().get(metricName);
            if (c != null && c.getCount() == expected) {
                break;
            }
            Thread.sleep(Duration.ofMillis(100).toMillis());
        }
    }
}
