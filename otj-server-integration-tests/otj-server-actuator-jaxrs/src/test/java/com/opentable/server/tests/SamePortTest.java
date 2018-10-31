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
