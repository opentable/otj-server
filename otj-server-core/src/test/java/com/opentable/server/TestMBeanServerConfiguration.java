/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.server;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.AnnotationMBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * This configuration class provides a non-static implementation of {@link MBeanServer} with a {@link Primary}
 * annotation. Include this configuration class in test Spring configurations where you want to create
 * multiple contexts; the annotation will ensure it overrides the default provision from
 * {@link JmxConfiguration}. Circumstances in which you might want to do this include integration testing.
 *
 * Note that this {@code MBeanServer} will not include the JVM's default registrations, such as for the
 * {@link java.lang.management.MemoryMXBean} or {@link java.lang.management.ThreadMXBean}.
 *
 * Note also that this works in Spring Boot 2.1 because the names are overridden as well as @Primary.
 * Also, the REPLACE_EXISTING policy is used to avoid duplicate MBeans across contexts.
 *
 * @see JmxConfiguration
 * @see MBeanServerTest
 */
@Configuration
@Import(TestMBeanServerConfiguration.InnerTestMBeanServerConfiguration.class)
public class TestMBeanServerConfiguration {
    @Configuration
    public static class InnerTestMBeanServerConfiguration {
        private final MBeanServer mbs = MBeanServerFactory.createMBeanServer();

        @Bean
        @Primary
        public MBeanServer getTestMBeanServer() {
            return mbs;
        }

        @Bean
        @Primary
        public AnnotationMBeanExporter mbeanExporter2() {
            final AnnotationMBeanExporter amber = new AnnotationMBeanExporter();
            amber.setServer(mbs);
            amber.setRegistrationPolicy(RegistrationPolicy.REPLACE_EXISTING);
            return amber;
        }
    }
}
