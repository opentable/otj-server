package com.opentable.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.context.annotation.Import;

import com.opentable.logging.CommonLogHolder;

@Import(RestTemplateAutoConfiguration.class)
public class BasicTest extends AbstractTest {

    @Test
    public void applicationLoads() {
        assertEquals("test", CommonLogHolder.getServiceType());
    }

    @Test
    public void httpServerInfoMatchesEnvironment() {
        assertEquals(port, httpServerInfo.getPort());
    }

    @Test
    public void testApiCall() {
        String res = restTemplate.getForObject("http://localhost:" + port + "/api/test", String.class);
        assertEquals("test", res);
    }
}
