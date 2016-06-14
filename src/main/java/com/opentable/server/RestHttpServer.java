package com.opentable.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.jackson.OpenTableJacksonConfiguration;
import com.opentable.pausedetector.EnablePauseDetector;

@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
    EmbeddedJetty.class,
    ResteasyAutoConfiguration.class,
    OpenTableJacksonConfiguration.class,
    //ServerLoggingConfiguration.class,
    ServerConfigConfiguration.class,
    StaticResourceConfiguration.class,
    MediocreHealthCheck.class,
    DiscoverySetup.class,
    StartupFailedHandler.class
})
@EnablePauseDetector
public @interface RestHttpServer {
}
