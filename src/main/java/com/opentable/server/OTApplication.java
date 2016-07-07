package com.opentable.server;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class OTApplication {
    public static ConfigurableApplicationContext run(Class<?> applicationClass, String[] args) {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        return SpringApplication.run(applicationClass, args);
    }
}
