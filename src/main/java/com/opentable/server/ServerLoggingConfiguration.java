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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;

import com.opentable.logging.AssimilateForeignLogging;
import com.opentable.logging.CommonLogHolder;

/* Registered via META-INF/spring.factories to capture early application lifecycle events */
class ServerLoggingConfiguration implements ApplicationListener<ApplicationEvent> {
    public static final String COMPONENT_NAME_KEY = "info.component"; // Taken from Dmitry's stack
    private static final Logger LOG = LoggerFactory.getLogger(ServerLoggingConfiguration.class);

    static {
        AssimilateForeignLogging.assimilate();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ApplicationEnvironmentPreparedEvent castEvent = (ApplicationEnvironmentPreparedEvent) event;
            String serviceName = castEvent.getEnvironment().getProperty(COMPONENT_NAME_KEY);
            if (StringUtils.isBlank(serviceName)) {
                serviceName = castEvent.getSpringApplication().getMainApplicationClass().getPackage().getImplementationTitle();
            }
            LOG.info("Setting service name to {}", serviceName);
            CommonLogHolder.setServiceType(serviceName);
        }
        if (event instanceof ApplicationFailedEvent) {
            LOG.info("Terminating default logging context");
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.reset();
            new BasicConfigurator().configure(loggerContext);
        }
    }
}
