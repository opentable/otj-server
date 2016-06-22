package com.opentable.server;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Maybe make this a more general locus in the future for all ManagementFactory, MBean, etc. Beans.

@Configuration
public class MBeanServerConfiguration {
    @Bean
    public MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }
}
