package com.opentable.server;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * This configuration class provides a non-static implementation of {@link MBeanServer} with a {@link Primary}
 * annotation. Include this configuration class in test Spring configurations where you want to create
 * multiple contexts; the annotation will ensure it overrides the default provision from
 * {@link JmxConfiguration}. Circumstances in which you might want to do this include integration testing.
 *
 * <p>
 * Note that this {@code MBeanServer} will not include the JVM's default registrations, such as for the
 * {@link java.lang.management.MemoryMXBean} or {@link java.lang.management.ThreadMXBean}.
 *
 * @see JmxConfiguration
 * @see MBeanServerTest
 */
@Configuration
public class TestMBeanServerConfiguration {
    @Bean
    @Primary
    public MBeanServer getTestMBeanServer() {
        return MBeanServerFactory.createMBeanServer();
    }
}
