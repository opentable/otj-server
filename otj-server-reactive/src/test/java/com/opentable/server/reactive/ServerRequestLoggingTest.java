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
package com.opentable.server.reactive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.opentable.logging.otl.HttpV1;
import com.opentable.server.reactive.utils.RequestLogInMemoryAppender;
// Check the RequestLogs function given various HTTP calls
//  The conserved header tests elsewhere complete most of the necessary tests
public class ServerRequestLoggingTest extends AbstractTest {

    @Autowired private TestRestTemplate testRestTemplate;

    private static RequestLogInMemoryAppender appender;

    @Before
    public void setUpLogging() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        appender = RequestLogInMemoryAppender.create();
    }

    @Test
    public void cantHandleVeryLargeHeader() {
        // First nothing special
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> res = testRestTemplate.exchange("/api/test", HttpMethod.GET, entity, String.class);
        assertEquals(200, res.getStatusCodeValue());

        // Now show we can handle a very small header
        headers.set("Payload", StringUtils.repeat('m', 50));
        entity = new HttpEntity<>(null, headers);
        res = testRestTemplate.exchange("/api/test", HttpMethod.GET, entity, String.class);
        assertEquals(200, res.getStatusCodeValue());


        // Now a sizeable one, but still not outrageous
        headers.set("Payload", StringUtils.repeat('m', 4096));
        entity = new HttpEntity<>(null, headers);
        res = testRestTemplate.exchange("/api/test", HttpMethod.GET, entity, String.class);
        assertEquals(200, res.getStatusCodeValue());

        // Now past jetty's default but under max
        headers.set("Payload", StringUtils.repeat('m', 9500));
        entity = new HttpEntity<>(null, headers);
        res = testRestTemplate.exchange("/api/test", HttpMethod.GET, entity, String.class);
        assertEquals(200, res.getStatusCodeValue());

        // Now break it. This depends on Abstract Test ot.httpserver.max-request-header-size=10000
        headers.set("Payload", StringUtils.repeat('m', 10001));
        entity = new HttpEntity<>(null, headers);
        res = testRestTemplate.exchange("/api/test", HttpMethod.GET, entity, String.class);
        assertEquals(431, res.getStatusCodeValue());
    }

    // Call an endpoint returning a single string, http 200
    @Test
    public void testNormalGet() throws InterruptedException {
        String res = testRestTemplate.getForObject("/api/test", String.class);
        assertEquals("test", res);

        // it would better to have some way to flush the appenders
        Thread.sleep(2000l);

        // get the last entry
        HttpV1 payload = appender.getEvents().get(appender.getEvents().size() - 1);
        assertEquals(200, payload.getStatus());
        assertEquals("/api/test", payload.getUrl());
        assertEquals(4, payload.getResponseSize());
        assertTrue(payload.getDuration() > 0);
    }

    // Everything should work fine for an "async" get
    @Test
    public void testAsyncGet() throws InterruptedException {
        String res = testRestTemplate.getForObject("/api/async", String.class);
        assertEquals("test", res);

        Thread.sleep(2000l);

        HttpV1 payload = appender.getEvents().get(appender.getEvents().size() - 1);
        assertEquals(200, payload.getStatus());
        assertEquals("/api/async", payload.getUrl());
        assertEquals(4, payload.getResponseSize());
        assertTrue(payload.getDuration() > 0);
    }

    // Nonexistent path
    @Test
    public void test404() throws InterruptedException {
        testRestTemplate.getForObject("/this/is/not/a/real/path", String.class);

        Thread.sleep(2000l);

        HttpV1 payload = appender.getEvents().get(appender.getEvents().size() - 1);
        assertEquals(404, payload.getStatus());
        assertEquals("/this/is/not/a/real/path", payload.getUrl());
        assertTrue(payload.getDuration() > 0);
    }
}
