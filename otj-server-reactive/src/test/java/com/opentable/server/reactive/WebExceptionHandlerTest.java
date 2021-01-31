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
package com.opentable.server.reactive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        // Call an endpoint that throws an exception
        EntityExchangeResult<ErrorResponse> result = webTestClient.get()
                .uri("/api/fault")
                .exchange()
                .expectBody(ErrorResponse.class)
                .returnResult();

        // Both MVC and WebFlux default to identical responses (The MVC test probes via JsonNode)
        ErrorResponse res = result.getResponseBody();
        assertNotNull(res);

        assertEquals("/api/fault", res.getPath());
        assertEquals(500, res.getStatus());
        assertEquals("Internal Server Error", res.getError());
        assertEquals("test", res.getMessage());
    }

    // Check what gets returned for ResponseStatusException being thrown
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
        assertTrue(res.getTimestamp().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }

    private static class ErrorResponse {

        private String timestamp;
        private String path;
        private int status;
        private String error;
        private String message;

        public String getTimestamp() {
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
