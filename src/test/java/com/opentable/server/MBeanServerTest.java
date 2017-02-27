package com.opentable.server;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.service.ServiceInfo;

/**
 * Test duplicate registration of MBeans in a context's {@link MBeanServer}. Having multiple contexts in the
 * same process is a very reasonable thing to do when performing, e.g., integration testing.
 *
 * @see TestMBeanServerConfiguration
 */
public class MBeanServerTest {
    /**
     * We test this to confirm the continued necessity of the added complexity of the
     * {@link TestMBeanServerConfiguration}.
     */
    @Test(expected = InstanceAlreadyExistsException.class)
    public void badDuplicate() throws Exception {
        registerDuplicate(BadTestConfiguration.class);
    }

    @Test
    public void goodDuplicate() throws Exception {
        registerDuplicate(GoodTestConfiguration.class);
    }

    private void registerDuplicate(final Class<?> configuration) throws Exception {
        final TestObjectMBean b = new TestObject();
        final ObjectName n = new ObjectName("com.example:type=TestMBean");
        register(configuration, b, n);
        register(configuration, b, n);
    }

    private void register(final Class<?> configuration, final Object obj, final ObjectName n)
            throws Exception {
        OTApplication.run(configuration, new String[]{})
                .getBean(MBeanServer.class)
                .registerMBean(obj, n);
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
}
