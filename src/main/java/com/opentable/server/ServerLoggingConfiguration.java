package com.opentable.server;

import org.springframework.context.annotation.Configuration;

import com.opentable.logging.AssimilateForeignLogging;

@Configuration
class ServerLoggingConfiguration {

    static {
        AssimilateForeignLogging.assimilate();
    }

}
