package com.opentable.server.tests;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.web.server.LocalManagementPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.opentable.logging.CommonLogHolder;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestMvcServer2Configuration.class, webEnvironment = RANDOM_PORT)
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
    private WebApplicationContext context;

    private MockMvc mvc;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .build();
    }

    @Test
    public void applicationLoads() {
        assertEquals("test", CommonLogHolder.getServiceType());
    }

    @Test
    public void portsAreSame() {
        assertEquals(port, managementPort);
    }


    @Test
    public void healthEndpoint() throws Exception {
        mvc
            .perform(
                get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/vnd.spring-boot.actuator.v2+json;charset=UTF-8"));
    }
}
