package com.opentable.server.reactive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Test for the configured default configured WebFlux {@link org.springframework.web.server.WebExceptionHandler}.
 */
public class WebExceptionHandlerTest extends AbstractTest {

    @Autowired
    WebTestClient webTestClient;

    @Test
    public void testExceptingCall() {
        EntityExchangeResult<ErrorResponse> result = webTestClient.get()
                .uri("/api/fault")
                .exchange()
                .expectBody(ErrorResponse.class)
                .returnResult();

        ErrorResponse res = result.getResponseBody();
        assertNotNull(res);

        assertEquals("/api/fault", res.getPath());
        assertEquals(500, res.getStatus());
        assertEquals("Internal Server Error", res.getError());
        assertEquals("test", res.getMessage());
    }

    @Test
    public void testSpecificExceptingCall() {
        EntityExchangeResult<ErrorResponse> result = webTestClient.get()
                .uri("/api/fault2")
                .exchange()
                .expectBody(ErrorResponse.class)
                .returnResult();

        ErrorResponse res = result.getResponseBody();
        assertNotNull(res);

        assertEquals("/api/fault2", res.getPath());
        assertEquals(502, res.getStatus());
        assertEquals("Bad Gateway", res.getError());
        assertEquals("test specific error", res.getMessage());
    }

    private static class ErrorResponse {

        private long timestamp;
        private String path;
        private int status;
        private String error;
        private String message;

        public long getTimestamp() {
            return timestamp;
        }

        public String getPath() {
            return path;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
}
