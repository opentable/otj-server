package com.opentable.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.opentable.logging.CommonLogHolder;

public class BasicTest extends AbstractTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

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
        String res = testRestTemplate.getForObject("/api/test", String.class);
        assertEquals("test", res);
    }

}
