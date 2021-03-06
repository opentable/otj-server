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
        "PORT_ACTUATOR=5562",
        "PORT_HTTP=5563",
        "ot.jmx.address=192.168.2.1",
        "PORT_MY-HTTPS=5564",
        "ot.jmx.port=5565",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "IS_KUBERNETES=TRUE",
        "ot.httpserver.active-connectors=default-http,my-https"
})
@DirtiesContext
public class PortSelectionWithNamedPortsInKubernetesTest {

    @Inject
    private ConfigurableEnvironment environment;

    @Test
    public void testPortSelection() {
        // In kubernetes so set to localhost
        Assert.assertEquals(PortSelectionWithInjectedOrdinalsWithoutKubernetesTest.LOCALHOST, environment.getProperty(PortSelector.JMX_ADDRESS));
        // Never set.
        Assert.assertNull(environment.getProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME));
        // Named ports for all of these!
        Assert.assertEquals(environment.getProperty("PORT_ACTUATOR"), environment.getProperty(PortSelector.MANAGEMENT_SERVER_PORT));
        Assert.assertEquals(environment.getProperty("PORT_HTTP"), environment.getProperty(PortSelector.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT));
        Assert.assertEquals(environment.getProperty("PORT_MY-HTTPS"), environment.getProperty("ot.httpserver.connector.my-https.port"));
        // Uses spring property, since named port not specified
        Assert.assertEquals("5565", environment.getProperty(PortSelector.JMX_PORT));

        SpringPortSelectionPostProcessor.OtPortSelectorPropertySource tt = (SpringPortSelectionPostProcessor.OtPortSelectorPropertySource)
                environment.getPropertySources().stream().filter(t -> t instanceof SpringPortSelectionPostProcessor.OtPortSelectorPropertySource).findFirst().orElse(null);
        Assert.assertNotNull(tt);
        Map<String, PortSelector.PortSelection> portSelectionMap = tt.getPortSelectionMap();
        Assert.assertEquals(4, portSelectionMap.size());
        // 3 named ports
        Assert.assertEquals(3, portSelectionMap.values().stream().filter(q -> q.getPortSource().equals(PortSelector.PortSource.FROM_PORT_NAMED)).count());
        // and then there's jmx...
        Assert.assertEquals(1, portSelectionMap.values().stream().filter(q -> q.getPortSource().equals(PortSelector.PortSource.FROM_SPRING_PROPERTY)).count());
    }
}
