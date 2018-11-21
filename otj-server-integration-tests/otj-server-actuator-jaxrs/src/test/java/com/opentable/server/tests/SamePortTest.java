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
package com.opentable.server.tests;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.opentable.logging.CommonLogHolder;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestJaxrsServer2Configuration.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
    "info.component=test",
    "ot.httpserver.active-connectors=boot"

})
public class SamePortTest {

    @LocalServerPort
    int port;

    @LocalManagementPort
    int managementPort;


    @Autowired
    private RestTemplate restTemplate;


    @Test
    public void applicationLoads() {
        assertEquals("test", CommonLogHolder.getServiceType());
    }

    @Test
    public void portsAreSame() {
        assertEquals(port, managementPort);
    }

    // Actuator not working without MVC https://opentable.atlassian.net/browse/OTPL-2897
    @Test(expected = HttpClientErrorException.class)
    public void healthEndpoint() throws Exception {
        restTemplate.getForObject("http://localhost:" + port + "/actuator/health", String.class);
    }

}
