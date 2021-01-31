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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
// Test standard endpoints for health, readiness, and service-status
public class HealthCheckTest extends AbstractTest {

    @Autowired
    public WebTestClient webTestClient;

    @Test
    public void testHealthEndpoint() {
        FluxExchangeResult<String> result = webTestClient.get().uri("/health").exchange().returnResult(String.class);
        assertEquals(HttpStatus.OK, result.getStatus());

        String response = result.getResponseBody().blockFirst();
        assertNotNull(response);
    }

    @Test
    public void testInfraHealthEndpoint() {
        FluxExchangeResult<String> result = webTestClient.get().uri("/infra/health").exchange().returnResult(String.class);
        assertEquals(HttpStatus.OK, result.getStatus());

        String response = result.getResponseBody().blockFirst();
        assertNotNull(response);
    }

    @Test
    public void testReadyEndpoint() {
        FluxExchangeResult<String> result = webTestClient.get().uri("/infra/ready").exchange().returnResult(String.class);
        assertEquals(HttpStatus.OK, result.getStatus());

        String response = result.getResponseBody().blockFirst();
        assertNotNull(response);
    }

    @Test
    public void testServiceStatusEndpoint() {
        FluxExchangeResult<String> result = webTestClient.get().uri("/service-status").exchange().returnResult(String.class);
        assertEquals(HttpStatus.OK, result.getStatus());

        String response = result.getResponseBody().blockFirst();
        assertNotNull(response);
    }

}
