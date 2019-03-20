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
package com.opentable.server.reactive.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import com.opentable.logging.JsonLogEncoder;

public class ApplicationLogInMemoryAppender extends AppenderBase<ILoggingEvent> {

    private static final JsonLogEncoder ENCODER = new JsonLogEncoder();
    private final List<ObjectNode> events = new ArrayList<>();

    public static ApplicationLogInMemoryAppender create(Class klass) throws NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender applicationLogInMemoryAppender = new ApplicationLogInMemoryAppender();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        applicationLogInMemoryAppender.setContext(loggerContext);
        applicationLogInMemoryAppender.start();
        Field logField = klass.getDeclaredField("LOG");
        logField.setAccessible(true);
        Logger applicationLogger = (Logger) logField.get(null);
        applicationLogger.addAppender(applicationLogInMemoryAppender);
        applicationLogger.setLevel(Level.ALL);
        applicationLogger.setAdditive(true);

        return applicationLogInMemoryAppender;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        events.add(ENCODER.convertToObjectNode(eventObject));
    }

    public List<ObjectNode> getEvents() {
        return events;
    }
}
