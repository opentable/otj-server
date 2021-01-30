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

        // Imports the managed bean below, proves it exported
        try (final ConfigurableApplicationContext ctx = OTApplication.run(TestApp.class, new String[] {})) {
            assertEquals("TestApp", mbs.getAttribute(name, "Name"));
        }

        try {
            mbs.getMBeanInfo(name);
            fail();
        } catch (@SuppressWarnings("unused") InstanceNotFoundException expected) {
            // ignore
        }
    }

    @Configuration
    @Import({TestServerConfiguration.class,
        MyManagedBean.class
    })
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
