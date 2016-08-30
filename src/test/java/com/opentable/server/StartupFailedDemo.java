package com.opentable.server;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class StartupFailedDemo {
    public static void main(String[] args) throws InterruptedException {
        try {
            SpringApplication.run(App.class, args);
        } catch (BeanCreationException e) {
            // No problem... wait for the T-1000.
            Thread.sleep(StartupShutdownFailedHandler.timeout.toMillis() * 2);
        }
        throw new RuntimeException("should never be reached");
    }

    @Configuration
    @RestHttpServer
    public static class App {
        @Configuration
        public static class BustedConfiguration {
            @Bean
            public Object boom() {
                throw new RuntimeException("boom");
            }
        }
    }
}
