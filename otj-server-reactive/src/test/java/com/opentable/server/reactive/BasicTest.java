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
    private WebTestClient webTestClient;

    // The context spins up and the service type is set
    @Test
    public void applicationLoads() {
        assertEquals("test", CommonLogHolder.getServiceType());
    }

    // The httpServerInfo is retrievable and matches local.server.port
    @Test
    public void httpServerInfoMatchesEnvironment() {
        assertEquals(port, httpServerInfo.getPort());
    }

    // A basic http get works
    @Test
    public void testApiCall() {
        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);
    }

}
