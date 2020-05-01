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
        "PORT0=5555",
        "PORT1=5556",
        "PORT2=5557",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "ot.httpserver.connector.default-http.port=-1",
        "IS_KUBERNETES=false"
})
@DirtiesContext
public class PortSelectionWithInjectedOrdinalsTest {
    static String LOCALHOST = "127.0.0.1";
    static String ASSIGN_NEXT_AVAILABLE = "-1";

    @Inject
    private ConfigurableEnvironment environment;

    @Test
    public void testPortSelection() {
        Assert.assertNull(environment.getProperty(JmxConfiguration.JmxmpServer.JAVA_RMI_SERVER_HOSTNAME));
        Assert.assertEquals(environment.getProperty("PORT2"), environment.getProperty(PortSelector.MANAGEMENT_SERVER_PORT));
        Assert.assertEquals(environment.getProperty("PORT0"), environment.getProperty(PortSelector.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT));
        Assert.assertEquals(environment.getProperty("PORT1"), environment.getProperty(PortSelector.JMX_PORT));

        SpringPortSelectionPostProcessor.OtPortSelectorPropertySource tt = (SpringPortSelectionPostProcessor.OtPortSelectorPropertySource)
                environment.getPropertySources().stream().filter(t -> t instanceof SpringPortSelectionPostProcessor.OtPortSelectorPropertySource).findFirst().orElse(null);
        Assert.assertNotNull(tt);
        Map<String, PortSelector.PortSelection> portSelectionMap = tt.getPortSelectionMap();
        Assert.assertEquals(3, portSelectionMap.size());
        Assert.assertEquals(3, portSelectionMap.values().stream().filter(q -> q.getPortSource().equals(PortSelector.PortSource.FROM_PORT_ORDINAL)).count());
    }
}
