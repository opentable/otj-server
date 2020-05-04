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
    //    "ot.jmx.port=44444",
   //     "management.server.port=50",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "IS_KUBERNETES=TRUE",
        "ot.httpserver.active-connectors=my-https,boot,default-http,fake"
})
@DirtiesContext
public class PortSelectionAllDefaultsInKubernetesTest {

    @Inject
    private ConfigurableEnvironment environment;

    @Test
    public void testPortSelection() {
        Assert.assertEquals(PortSelectionWithInjectedOrdinalsWithoutKubernetesTest.LOCALHOST, environment.getProperty(PortSelector.JMX_ADDRESS));
        Assert.assertNull(environment.getProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME));
        Assert.assertNull(environment.getProperty(PortSelector.MANAGEMENT_SERVER_PORT));
        Assert.assertEquals("8080", environment.getProperty(PortSelector.SERVER_PORT));
        Assert.assertEquals("0", environment.getProperty(PortSelector.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT));
        Assert.assertEquals("0", environment.getProperty("ot.httpserver.connector.my-https.port"));
        Assert.assertEquals("0", environment.getProperty("ot.httpserver.connector.fake.port"));
        Assert.assertEquals("0", environment.getProperty(PortSelector.JMX_PORT));

        SpringPortSelectionPostProcessor.OtPortSelectorPropertySource tt = (SpringPortSelectionPostProcessor.OtPortSelectorPropertySource)
                environment.getPropertySources().stream().filter(t -> t instanceof SpringPortSelectionPostProcessor.OtPortSelectorPropertySource).findFirst().orElse(null);
        Assert.assertNotNull(tt);
        Map<String, PortSelector.PortSelection> portSelectionMap = tt.getPortSelectionMap();
        Assert.assertEquals(6, portSelectionMap.size());
        Assert.assertEquals(5,
                portSelectionMap.values().stream().filter(q -> q.getPortSource().equals(PortSelector.PortSource.FROM_DEFAULT_VALUE)).count());
    }
}
