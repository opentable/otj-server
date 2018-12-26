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
import static org.junit.Assert.assertNotEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import com.opentable.logging.CommonLogHolder;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestJaxrsServer2Configuration.class, SeparatePortTest.EventListenerConfiguration.class}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
    "info.component=test",
    "ot.httpserver.active-connectors=boot",
    "management.server.port=0"

})
public class SeparatePortTest {

    @LocalServerPort
    int port;

    @LocalManagementPort
    int managementPort;


    @Autowired
    private RestTemplate restTemplate;

    private static WebApplicationContext context;


    @Configuration
    public static class EventListenerConfiguration {
        @EventListener
        public void injectEmbeddedServletContainer(WebServerInitializedEvent e) {
            context = Optional.ofNullable(e.getApplicationContext())
                .map(WebServerApplicationContext.class::cast)
                .filter(i -> "management".equals(i.getServerNamespace()))
                .map(WebApplicationContext.class::cast)
                .orElse(context);
        }
    }

    @Test
    public void applicationLoads() {
        assertEquals("test", CommonLogHolder.getServiceType());
    }

    @Test
    public void portsAreDifferent() {
        assertNotEquals(port, managementPort);
    }

    // Actuator not working without MVC https://opentable.atlassian.net/browse/OTPL-2897
    @Test(expected = HttpClientErrorException.class)
    public void healthEndpoint() {
        restTemplate.getForObject("http://localhost:" + managementPort + "/actuator/health", String.class);
    }

}
