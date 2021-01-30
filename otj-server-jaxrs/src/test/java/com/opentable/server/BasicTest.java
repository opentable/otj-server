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
    TestJaxRsServerConfiguration.class
})
@TestPropertySource(properties = {
        "ot.httpserver.max-threads=13",
        "ot.httpserver.static-path=static-test"
})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class BasicTest {
    @Inject
    JAXRSLoopbackRequest request;

    @Inject
    EmbeddedJetty ej;

    @Inject
    MetricRegistry metrics;

    // If the path returns a 500, show that and that response is correct
    // and that the metrics update
    @Test(timeout = 10_000)
    public void test5xx() throws InterruptedException {
        String responseText = request.of("/5xx").request().get().readEntity(String.class);
        assertEquals(TestJaxRsServerConfiguration.TestErrorHandler.TEXT, responseText);
        waitForCount("http-server.500-responses", 1);
    }

    // Normal endpoint returns 200, plus metrics update
    @Test(timeout = 10_000)
    public void testHello() throws InterruptedException {
        assertEquals(TestJaxRsServerConfiguration.HELLO_WORLD, request.of("/").request().get().readEntity(String.class));
        waitForCount("http-server.200-responses", 1);
    }

    // non mapped resource returns a 404, updates the metrics
    @Test(timeout = 10_000)
    public void testMissing() throws InterruptedException {
        try(Response r = request.of("/not/found/omg/wtf/bbq").request().get()){
            assertEquals(404, r.getStatus());
            waitForCount("http-server.404-responses", 1);
        }
    }


    // Test static resource configuration
    @Test
    public void testStatic_txt() throws IOException {
        testStatic("static-test/test.txt", "text/plain");
    }

    @Test
    public void testStatic_png() throws IOException {
        testStatic("static-test/test.png", "image/png");
    }

    // Show configuration of thread pool is correct
    @Test
    public void testPoolCustomizations() throws Exception {
        QueuedThreadPool qtp = ej.getThreadPool();
        assertEquals(13, qtp.getMinThreads());
        assertEquals(13, qtp.getMaxThreads());
    }

    private void testStatic(final String path, final String expectedContentType) throws IOException {
        final byte[] expected = IOUtils.toByteArray(ClassLoader.getSystemResourceAsStream(path));
        try(final Response r = request.of(path).request(expectedContentType).get()){
            assertEquals(expectedContentType, r.getHeaderString(HttpHeaders.CONTENT_TYPE));
            final byte[] actual = r.readEntity(byte[].class);
            Assert.assertArrayEquals(expected, actual);
        }
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
