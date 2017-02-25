package com.opentable.server;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opentable.service.ServiceInfo;

public class MBeanServerTest {
    @Test
    public void testDuplicate() throws Exception {
        final TestObjectMBean b = new TestObject();
        final ObjectName n = new ObjectName("com.example:type=TestMBean");
        register(b, n);
        register(b, n);
    }

    private void register(final Object obj, final ObjectName n) throws Exception {
        OTApplication.run(TestConfiguration.class, new String[]{})
                .getBean(MBeanServer.class)
                .registerMBean(obj, n);
    }

    public interface TestObjectMBean {}

    static class TestObject implements TestObjectMBean {}

    @Configuration
    @RestHttpServer
    static class TestConfiguration {
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
}
