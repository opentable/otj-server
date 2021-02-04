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
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.opentable.httpheaders.OTHeaders;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestReactiveServerConfiguration.class, OverrideConfiguration.class}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
        "info.component=test",
        "OT-Claims=foo"
})
@DirtiesContext
public class OverrideConservedHeadersTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testApiCallConservesHeadersButOverridesAsNeeded() {
        final String requestId = UUID.randomUUID().toString();
        final String anonId = UUID.randomUUID().toString();
        final String claimId = UUID.randomUUID().toString();

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/conservedclaims")
                .header(OTHeaders.REQUEST_ID, requestId)
                .header(OTHeaders.ANONYMOUS_ID, anonId)
                .header(OTHeaders.CLAIMS_ID, claimId)
                .header("Not-A-Conserved-Header", "some value")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(OTHeaders.REQUEST_ID, requestId)
                .expectHeader().valueEquals(OTHeaders.ANONYMOUS_ID, anonId)
                .expectHeader().doesNotExist("Not-A-Conserved-Header")
                .expectBody(String.class)
                .returnResult();


        final String res = result.getResponseBody();
        assertEquals("foo", res);
    }

}
