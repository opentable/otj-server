package com.opentable.server.reactive;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.opentable.logging.CommonLogHolder;

/**
 * Basic test of a {@link ReactiveServer}.
 */
public class BasicTest extends AbstractTest {

    @Autowired
    WebTestClient webTestClient;

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
        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .exchange()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);
    }

}
