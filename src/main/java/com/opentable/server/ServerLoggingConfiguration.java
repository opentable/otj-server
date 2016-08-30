package com.opentable.server;

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
    private static final Logger LOG = LoggerFactory.getLogger(ServerLoggingConfiguration.class);

    static {
        AssimilateForeignLogging.assimilate();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            ApplicationEnvironmentPreparedEvent castEvent = (ApplicationEnvironmentPreparedEvent) event;
            String serviceName = castEvent.getSpringApplication().getMainApplicationClass().getPackage().getImplementationTitle();
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
