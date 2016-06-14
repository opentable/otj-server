package com.opentable.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.BasicConfigurator;
import ch.qos.logback.classic.LoggerContext;

import com.opentable.logging.AssimilateForeignLogging;

@Configuration
class ServerLoggingConfiguration implements ApplicationListener<ApplicationFailedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(ServerLoggingConfiguration.class);

    static {
        AssimilateForeignLogging.assimilate();
    }

    @Override
    public void onApplicationEvent(ApplicationFailedEvent event) {
        LOG.info("Terminating default logging context");
        ((LoggerContext) LoggerFactory.getILoggerFactory()).reset();
        BasicConfigurator.configureDefaultContext();
    }

}
