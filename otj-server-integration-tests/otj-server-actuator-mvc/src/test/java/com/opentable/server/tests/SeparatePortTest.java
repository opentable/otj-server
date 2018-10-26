package com.opentable.server.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.opentable.logging.CommonLogHolder;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestMvcServer2Configuration.class, SeparatePortTest.EventListenerConfiguration.class}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
    "info.component=test",
    "ot.httpserver.active-connectors=boot",
    "management.server.port=0"

})
public class SeparatePortTest {

    @LocalServerPort
    private int port;

    @LocalManagementPort
    private int managementPort;

    private static WebApplicationContext context;

    private MockMvc mvc;

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
    public void portsAreDifferent() {
        assertNotEquals(port, managementPort);
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
