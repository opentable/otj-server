package com.opentable.server;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.UnableToRegisterMBeanException;

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
    @Test(expected = InstanceAlreadyExistsException.class)
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

    private void registerDuplicate(final Class<?> configuration, final Registrar reg) throws Exception {
        final TestObjectMBean b = new TestObject();
        final ObjectName n = new ObjectName("com.example:type=TestMBean");
        final ConfigurableApplicationContext ctx1 = run(configuration);
        reg.register(ctx1, b, n);
        reg.register(run(configuration), b, n);
    }

    private ConfigurableApplicationContext run(final Class<?> configuration) {
        return OTApplication.run(configuration, new String[]{});
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

    // Uses Spring's internal machinery for registering MBeans, which is different from naive bean registry.
    private void registerSpring(
            final ConfigurableApplicationContext ctx,
            final Object obj,
            final ObjectName n) throws Exception {
        ctx.getBean(MBeanExporter.class).registerManagedResource(obj, n);
    }

    /**
     * Example minimal MBean.
     */
    public interface TestObjectMBean {}
    static class TestObject implements TestObjectMBean {}

    /**
     * Server configuration looks fine, right?  Well, not if you want to register any MBeans.  By default,
     * {@link JmxConfiguration} will inject an {@link MBeanServer} that's static.
     *
     * @see GoodTestConfiguration
     */
    @Configuration
    @RestHttpServer
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
     * {@link MBeanServer}.
     *
     * @see TestMBeanServerConfiguration
     */
    @Import(TestMBeanServerConfiguration.class)
    static class GoodTestConfiguration extends BadTestConfiguration {}

    private interface Registrar {
        void register(ConfigurableApplicationContext ctx, final Object obj, final ObjectName n) throws Exception;
    }
}
