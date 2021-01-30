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
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Stack;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.RequestLogEvent;
import com.opentable.logging.otl.HttpV1;
// As the name implies, with the help of an inmemory appender, tests request logging.
public class RequestLoggingTest extends AbstractTest {

    @Autowired private TestRestTemplate testRestTemplate;

    private static final InMemoryAppender APPENDER = new InMemoryAppender();

    @Before
    public void setUpLogging() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        APPENDER.setContext(loggerContext);
        APPENDER.start();
        Field logField = JsonRequestLog.class.getDeclaredField("LOG");
        logField.setAccessible(true);
        Logger requestLogger = (Logger) logField.get(null);
        requestLogger.addAppender(APPENDER);
        requestLogger.setLevel(Level.ALL);
        requestLogger.setAdditive(true);
    }

    @Test
    public void testNormalGet() throws InterruptedException {
        String res = testRestTemplate.getForObject("/api/test", String.class);
        assertEquals("test", res);
        Thread.sleep(2000l);
        RequestLogEvent loggingEvent = (RequestLogEvent) APPENDER.eventStack.pop();
        HttpV1 payload = loggingEvent.getPayload();
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
        RequestLogEvent loggingEvent = (RequestLogEvent) APPENDER.eventStack.pop();
        HttpV1 payload = loggingEvent.getPayload();
        assertEquals(200, payload.getStatus());
        assertEquals("/api/async", payload.getUrl());
        assertEquals(4, payload.getResponseSize());
        assertTrue(payload.getDuration() > 0);
    }

    @Test
    public void test404() throws InterruptedException {
        testRestTemplate.getForObject("/this/is/not/a/real/path", String.class);
        Thread.sleep(2000l);
        RequestLogEvent loggingEvent = (RequestLogEvent) APPENDER.eventStack.pop();
        HttpV1 payload = loggingEvent.getPayload();
        assertEquals(404, payload.getStatus());
        assertEquals("/this/is/not/a/real/path", payload.getUrl());
        assertEquals("GET", payload.getMethod());
        assertTrue(payload.getResponseSize() > 100);
        assertTrue(payload.getDuration() > 0);
    }

    // Spring issue with 404 POST not returning correctly
    // Corrected in Spring 2.2.11+
    @Test
    public void test404Post() throws InterruptedException {
        testRestTemplate.postForObject("/this/is/not/a/real/path", null, String.class);
        Thread.sleep(2000l);
        RequestLogEvent loggingEvent = (RequestLogEvent) APPENDER.eventStack.pop();
        HttpV1 payload = loggingEvent.getPayload();
        assertEquals(404, payload.getStatus());
        assertEquals("/this/is/not/a/real/path", payload.getUrl());
        assertEquals("POST", payload.getMethod());
        assertTrue(payload.getResponseSize() > 100);
        assertTrue(payload.getDuration() > 0);
    }

    public static class InMemoryAppender extends AppenderBase<ILoggingEvent> {

        public Stack<ILoggingEvent> eventStack = new Stack<>();

        @Override
        protected void append(ILoggingEvent eventObject) {
            eventStack.add(eventObject);
        }

    }
}

