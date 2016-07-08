package com.opentable.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import com.opentable.server.JmxExportTest.TestApp.MyManagedBean;
import com.opentable.service.ServiceInfo;

public class JmxExportTest {
    @Test
    public void testJmxExport() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = new ObjectName(
                "com.opentable.server:name=com.opentable.server.JmxExportTest$TestApp$MyManagedBean,"
                        + "type=JmxExportTest.TestApp.MyManagedBean");

        try (final ConfigurableApplicationContext ctx = OTApplication.run(TestApp.class, new String[] {})) {
            assertEquals("TestApp", mbs.getAttribute(name, "Name"));
        }

        try {
            mbs.getMBeanInfo(name);
            fail();
        } catch (InstanceNotFoundException expected) {
            // ignore
        }
    }

    @Configuration
    @RestHttpServer
    @Import(MyManagedBean.class)
    public static class TestApp {
        @ManagedResource
        public static class MyManagedBean {
            @ManagedAttribute
            public String getName() {
                return "TestApp";
            }
        }

        @Bean
        ServiceInfo serviceInfo() {
            return new ServiceInfo() {
                @Override
                public String getName() {
                    return "test";
                }
            };
        }
    }
}
