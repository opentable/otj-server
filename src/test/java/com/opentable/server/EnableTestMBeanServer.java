package com.opentable.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.Import;

/**
 * Provide non-static {@link javax.management.MBeanServer} and {@link org.springframework.jmx.export.MBeanExporter}
 * using that server for test cases. Test cases often run multiple Spring contexts in the same process, and so
 * duplicate registrations can cause the tests to fail. Using a non-static server resolves this.
 *
 * @see JmxConfiguration
 * @see MBeanServerTest
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableMBeanExport
@Import(TestMBeanServerConfiguration.class)
public @interface EnableTestMBeanServer {
}
