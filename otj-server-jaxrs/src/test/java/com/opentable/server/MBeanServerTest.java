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
import javax.management.ObjectName;

import com.codahale.metrics.health.HealthCheck;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.UnableToRegisterMBeanException;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.opentable.server.jaxrs.JAXRSServer;
import com.opentable.service.ServiceInfo;

/**
 * Test duplicate registration of MBeans in a context's {@link MBeanServer}. Having multiple contexts in the
 * same process is a very reasonable thing to do when performing, e.g., integration testing.
 *
 * @see TestMBeanServerConfiguration
 */
public class MBeanServerTest {
    /**
     * We test these to confirm the continued necessity of the added complexity of the
     * {@link TestMBeanServerConfiguration}.
     */
    @Test(expected = UnableToRegisterMBeanException.class)
    public void badDuplicateSimple() throws Exception {
        registerDuplicate(BadTestConfiguration.class, this::registerSimple);
    }
    @Test(expected = UnableToRegisterMBeanException.class)
    public void badDuplicateSpring() throws Exception {
        registerDuplicate(BadTestConfiguration.class, this::registerSpring);
    }

    @Test
    public void goodDuplicateSimple() throws Exception {
        registerDuplicate(GoodTestConfiguration.class, this::registerSimple);
    }

    @Test
    public void goodDuplicateSpring() throws Exception {
        registerDuplicate(GoodTestConfiguration.class, this::registerSpring);
    }

    /**
     * We test these to confirm the continued necessity of including the {@link TestMBeanServerConfiguration} later.
     *
     * @see TestMBeanServerConfiguration
     */
    @Test(expected = UnableToRegisterMBeanException.class)
    public void shouldWorkButDoesNotSimple() throws Exception {
        registerDuplicate(ShouldBeGoodButIsNotConfiguration.class, this::registerSimple);
    }
    @Test(expected = UnableToRegisterMBeanException.class)
    public void shouldWorkButDoesNotSpring() throws Exception {
        registerDuplicate(ShouldBeGoodButIsNotConfiguration.class, this::registerSpring);
    }

    private void registerDuplicate(final Class<?> configuration, final Registrar reg) throws Exception {
        final TestObjectMBean b = new TestObject();
        final ObjectName n = new ObjectName("com.example:type=TestMBean");
        try(final ConfigurableApplicationContext ctx1 = OTApplication.run(configuration)){
            reg.register(ctx1, b, n);
            reg.register(OTApplication.run(configuration), b, n);
        }
    }

    /**
     * This ends up testing fetching a context-specific MBeanServer as a *bean*--like the one we provide in
     * {@link JmxConfiguration}, and not from Spring's internal machinery.
     */
    private void registerSimple(
            final ConfigurableApplicationContext ctx,
            final Object obj,
            final ObjectName n) throws Exception {
        ctx.getBean(MBeanServer.class).registerMBean(obj, n);
    }

    /** Uses Spring's internal machinery for registering MBeans, which is different from naive bean registry. */
    private void registerSpring(
            final ConfigurableApplicationContext ctx,
            final Object obj,
            final ObjectName n) {
        ctx.getBean(MBeanExporter.class).registerManagedResource(obj, n);
    }

    /**
     * Example minimal MBean.
     */
    public interface TestObjectMBean {}
    static class TestObject implements TestObjectMBean {}

    /**
     * Example MBean requiring the {@link org.springframework.jmx.export.annotation.AnnotationMBeanExporter}.
     */
    @ManagedResource
    public static class ManagedHealthCheck extends HealthCheck {
        private volatile boolean healthy = true;

        @Override
        protected Result check() {
            return healthy ? Result.healthy() : Result.unhealthy("someone set up us the bomb");
        }

        @ManagedAttribute
        public void setHealthy(final boolean healthy) {
            this.healthy = healthy;
        }

        @ManagedAttribute
        public boolean isHealthy() {
            return healthy;
        }
    }

    /**
     * Server configuration looks fine, right?  Well, not if you want to register any MBeans.  By default,
     * {@link JmxConfiguration} will inject an {@link MBeanServer} that's static.
     *
     * @see GoodTestConfiguration
     * @see ShouldBeGoodButIsNotConfiguration
     */
    @Configuration
    @JAXRSServer
    @Import(ManagedHealthCheck.class)
    static class BadTestConfiguration {
        @Bean
        ServiceInfo getServiceInfo() {
            return new ServiceInfo() {
                @Override
                public String getName() {
                    return "test-service";
                }
            };
        }
    }

    /**
     * In order to run multiple contexts in the same process and register MBeans without the server
     * complaining about duplicate registration, you'll need to override the provision of the
     * {@link MBeanServer} and {@link MBeanExporter}.
     *
     * <p>
     * Note that the {@link TestMBeanServerConfiguration} comes <am>after</am> the service configuration.
     *
     * @see TestMBeanServerConfiguration
     */
    @Configuration
    @Import({
            BadTestConfiguration.class,
            TestMBeanServerConfiguration.class,
    })
    static class GoodTestConfiguration {
    }

    /**
     * Alas, you'd think this would work too, but it doesn't. This has to do with a combination of Spring's
     * bean declaration order-sensitivity and how the {@link MBeanExporter} is initialized.
     *
     * @see TestMBeanServerConfiguration
     */
    @Configuration
    @Import({
            TestMBeanServerConfiguration.class,
            BadTestConfiguration.class,
    })
    static class ShouldBeGoodButIsNotConfiguration {
    }

    private interface Registrar {
        void register(ConfigurableApplicationContext ctx, final Object obj, final ObjectName n) throws Exception;
    }
}
