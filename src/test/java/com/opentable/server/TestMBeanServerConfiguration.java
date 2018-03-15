package com.opentable.server;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * Note that this {@code MBeanServer} will not include the JVM's default registrations, such as for the
 * {@link java.lang.management.MemoryMXBean} or {@link java.lang.management.ThreadMXBean}.
 *
 * <p>
 * Note too the overriding of Spring's {@link MBeanExporter}.
 *
 * <p>
 * This isn't <em>really</em> deprecated; the reason the annotation is present is to let you know that you should not
 * import this configuration class by itself. Due to runtime checks implemented by the
 * {@link MBeanExportConfiguration}, it needs to be imported by a class that uses the
 * {@link org.springframework.context.annotation.EnableMBeanExport} annotation.  Our own {@link EnableTestMBeanServer}
 * annotation serves this purpose, and so you should just use it instead.
 *
 * @see EnableTestMBeanServer
 * @see JmxConfiguration
 * @see MBeanServerTest
 */
@Deprecated // See Javadoc.
@Configuration
public class TestMBeanServerConfiguration extends MBeanExportConfiguration {
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
