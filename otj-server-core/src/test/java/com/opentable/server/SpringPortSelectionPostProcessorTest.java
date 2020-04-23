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

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles(profiles = "deployed")
@ContextConfiguration(classes = {
        TestServerConfiguration.class
})
@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "PORT_ACTUATOR=9999",
        "PORT_HTTP=9998",
        "PORT_MY-HTTPS=9997",
        "PORT_JMX=9996",
//        "management.server.port=50",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "IS_KUBERNETES=TRUE"
})
public class SpringPortSelectionPostProcessorTest {

    @Inject
    private Environment environment;

    @Test
    // Shows PORT_ACTUATOR is picked up and takes, but has lowest priority
    public void testActuator() {
        Assert.assertEquals("9999", environment.getProperty(SpringPortSelectionPostProcessor.MANAGEMENT_SERVER_PORT));
        Assert.assertEquals("9998", environment.getProperty(SpringPortSelectionPostProcessor.SERVER_PORT));
        Assert.assertEquals("9998", environment.getProperty(SpringPortSelectionPostProcessor.HTTPSERVER_CONNECTOR_DEFAULT_HTTP_PORT));
        Assert.assertEquals("9997", environment.getProperty("ot.httpserver.connector.my-https.port"));
        Assert.assertEquals("9996", environment.getProperty(SpringPortSelectionPostProcessor.JMX_PORT));
    }
}