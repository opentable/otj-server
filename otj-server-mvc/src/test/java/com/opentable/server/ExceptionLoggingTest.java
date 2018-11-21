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

import java.util.Stack;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.opentable.server.mvc.LoggingHandlerExceptionResolver;

public class ExceptionLoggingTest extends AbstractTest {

    @Autowired private TestRestTemplate testRestTemplate;

    private static final InMemoryAppender APPENDER = new InMemoryAppender();

    @Before
    public void setUpLogging() throws SecurityException, IllegalArgumentException {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        APPENDER.setContext(loggerContext);
        APPENDER.start();
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingHandlerExceptionResolver.class);
        logger.addAppender(APPENDER);
        logger.setLevel(Level.ALL);
        logger.setAdditive(true);

    }

    @Test
    public void testMethodArgumentTypeMismatchExceptionIsLogged() throws InterruptedException {
        String res = testRestTemplate.getForObject("/api/request-param?date=1234", String.class);
        Thread.sleep(2000l);
        LoggingEvent loggingEvent = (LoggingEvent) APPENDER.eventStack.pop();
        assertTrue(res.contains("\"status\":400"));
        assertEquals("", loggingEvent.getMessage());
        assertEquals("org.springframework.web.method.annotation.MethodArgumentTypeMismatchException", loggingEvent.getThrowableProxy().getClassName());
    }

    @Test
    public void testExceptionInMethodLoggedOnlyOnce() throws InterruptedException {
        String res = testRestTemplate.getForObject("/api/fault", String.class);
        Thread.sleep(2000l);
        assertEquals(0, APPENDER.eventStack.size());
        assertTrue(res.contains("\"status\":500"));
    }

    @Test
    public void testHttpMessageNotReadableExceptionExceptionIsLogged() throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String res = testRestTemplate.postForObject("/api/map",
            new HttpEntity<>("###", headers), String.class);
        Thread.sleep(2000l);
        LoggingEvent loggingEvent = (LoggingEvent) APPENDER.eventStack.pop();
        assertTrue(res.contains("\"status\":400"));
        assertEquals("", loggingEvent.getMessage());
        assertEquals("org.springframework.http.converter.HttpMessageNotReadableException", loggingEvent.getThrowableProxy().getClassName());
    }

    public static class InMemoryAppender extends AppenderBase<ILoggingEvent> {
        public Stack<ILoggingEvent> eventStack = new Stack<>();
        @Override
        protected void append(ILoggingEvent eventObject) {
            eventStack.add(eventObject);
        }
    }
}

