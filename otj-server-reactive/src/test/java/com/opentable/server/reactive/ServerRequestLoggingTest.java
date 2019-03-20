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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.opentable.logging.otl.HttpV1;
import com.opentable.server.reactive.utils.RequestLogInMemoryAppender;

public class ServerRequestLoggingTest extends AbstractTest {

    @Autowired private TestRestTemplate testRestTemplate;

    private static RequestLogInMemoryAppender appender;

    @Before
    public void setUpLogging() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        appender = RequestLogInMemoryAppender.create();
    }

    @Test
    public void testNormalGet() throws InterruptedException {
        String res = testRestTemplate.getForObject("/api/test", String.class);
        assertEquals("test", res);

        Thread.sleep(2000l);

        HttpV1 payload = appender.getEvents().get(appender.getEvents().size() - 1);
        assertEquals(200, payload.getStatus());
        assertEquals("/api/test", payload.getUrl());
        assertEquals(4, payload.getResponseSize());
        assertTrue(payload.getDuration() > 0);
    }

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
