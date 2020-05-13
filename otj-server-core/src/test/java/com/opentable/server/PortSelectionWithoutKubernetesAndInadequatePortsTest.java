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

import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles(profiles = SpringPortSelectionPostProcessor.PROCESSOR_TEST)
@ContextConfiguration(classes = {
        TestServerConfiguration.class
})
@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "PORT0=5567",
        "PORT1=5568",
        "PORT2=5569",
        "PORT_ACTUATOR=5570",
        "PORT_HTTP=5571",
        "PORT_MY-HTTPS=5572",
        "PORT_JMX=5573",
        "management.server.port=44444",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "IS_KUBERNETES=false",
        "ot.httpserver.active-connectors=default-http,boot,my-https",
        "ot.components.features.otj-actuator.enabled=true"

})
@DirtiesContext
public class PortSelectionWithoutKubernetesAndInadequatePortsTest {

    @Inject
    private ConfigurableEnvironment environment;

    @Test
    public void testPortSelection() {
        // Not kubernetes, and not set.
        Assert.assertNull(environment.getProperty(PortSelector.JMX_ADDRESS));
        // Never set
        Assert.assertNull(environment.getProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME));
        // In singularity, spring property takes precedence over ordinals
        Assert.assertEquals("44444", environment.getProperty(PortSelector.MANAGEMENT_SERVER_PORT));
        // No spring properties for these, so allocated in this order
        Assert.assertEquals(environment.getProperty("PORT1"), environment.getProperty(PortSelector.SERVER_PORT));
        Assert.assertEquals(environment.getProperty("PORT0"), environment.getProperty(PortSelector.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT));
        Assert.assertEquals(environment.getProperty("PORT2"), environment.getProperty("ot.httpserver.connector.my-https.port"));
        // Just proves the obvious
        Assert.assertNull( environment.getProperty("ot.httpserver.connector.fake.port"));
        // We don't have a port, fall back to default
        Assert.assertEquals("0", environment.getProperty(PortSelector.JMX_PORT));

        SpringPortSelectionPostProcessor.OtPortSelectorPropertySource tt = (SpringPortSelectionPostProcessor.OtPortSelectorPropertySource)
                environment.getPropertySources().stream().filter(t -> t instanceof SpringPortSelectionPostProcessor.OtPortSelectorPropertySource).findFirst().orElse(null);
        Assert.assertNotNull(tt);
        Map<String, PortSelector.PortSelection> portSelectionMap = tt.getPortSelectionMap();
        Assert.assertEquals(5, portSelectionMap.size());

        Assert.assertEquals(3, portSelectionMap.values().stream().filter(q -> q.getPortSource().equals(PortSelector.PortSource.FROM_PORT_ORDINAL)).count());
        // set as spring property (Actuator), which wins over ordinal ports.
        Assert.assertEquals(1, portSelectionMap.values().stream().filter(q -> q.getPortSource().equals(PortSelector.PortSource.FROM_SPRING_PROPERTY)).count());
        // default value for JMX_PORT since we ran out of ordinal and it has a default
        Assert.assertEquals(1, portSelectionMap.values().stream().filter(q -> q.getPortSource().equals(PortSelector.PortSource.FROM_DEFAULT_VALUE)).count());

    }
}
