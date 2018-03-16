package com.opentable.server;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.MBeanExportConfiguration;
import org.springframework.context.annotation.Primary;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;

/**
 * This configuration class provides a non-static implementation of {@link MBeanServer} with a {@link Primary}
 * annotation. Include this configuration class in test Spring configurations where you want to create
 * multiple contexts; the annotation will ensure it overrides the default provision from
 * {@link JmxConfiguration}. Circumstances in which you might want to do this include integration testing.
 *
 * <p>
 * It is critical that this configuration class be imported <em>later</em> rather than earlier. This is an unfortunate
 * consequence of two aspects of Spring. 1. If there are two beans of the same name and type, the latter one silently
 * wins. 2. The Spring {@link MBeanExporter} initialization does not use the vanilla IoC setup.
 *
 * <p>
 * Note that this {@code MBeanServer} will not include the JVM's default registrations, such as for the
 * {@link java.lang.management.MemoryMXBean} or {@link java.lang.management.ThreadMXBean}.
 *
 * @see JmxConfiguration
 * @see MBeanServerTest
 */
@Configuration
@EnableMBeanExport
@Import(TestMBeanServerConfiguration.InnerTestMBeanServerConfiguration.class)
public class TestMBeanServerConfiguration {
    @Configuration
    public static class InnerTestMBeanServerConfiguration extends MBeanExportConfiguration {
        private final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        @Bean
        @Primary
        public MBeanServer getTestMBeanServer() {
            return mbs;
        }

        @Override
        public AnnotationMBeanExporter mbeanExporter() {
            final AnnotationMBeanExporter amber = super.mbeanExporter();
            amber.setServer(mbs);
            return amber;
        }
    }
}
