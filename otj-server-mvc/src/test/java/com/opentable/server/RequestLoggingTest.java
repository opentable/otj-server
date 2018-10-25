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

