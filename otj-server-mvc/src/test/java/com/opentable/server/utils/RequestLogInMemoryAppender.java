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
package com.opentable.server.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.opentable.logging.jetty.JsonRequestLog;
import com.opentable.logging.jetty.RequestLogEvent;
import com.opentable.logging.otl.HttpV1;

public class RequestLogInMemoryAppender extends AppenderBase<ILoggingEvent> {

    private final List<HttpV1> events = new ArrayList<>();

    public static RequestLogInMemoryAppender create() throws NoSuchFieldException, IllegalAccessException {
        RequestLogInMemoryAppender requestLogAppender = new RequestLogInMemoryAppender();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        requestLogAppender.setContext(loggerContext);
        requestLogAppender.start();
        Field logField = JsonRequestLog.class.getDeclaredField("LOG");
        logField.setAccessible(true);
        Logger requestLogger = (Logger) logField.get(null);
        requestLogger.addAppender(requestLogAppender);
        requestLogger.setLevel(Level.ALL);
        requestLogger.setAdditive(true);

        return requestLogAppender;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        events.add(((RequestLogEvent) eventObject).getPayload());
    }

    public List<HttpV1> getEvents() {
        return events;
    }
}
