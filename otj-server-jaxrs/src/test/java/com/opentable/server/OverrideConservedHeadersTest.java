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
package com.opentable.server;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.UUID;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.conservedheaders.ConservedHeader;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestJaxRsServerConfiguration.class, OverrideConfiguration.class
}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
        "info.component=test",
        "ot.httpserver.active-connectors=boot",
        "OT-Claims=foo",
})
@DirtiesContext
public class OverrideConservedHeadersTest {

    private static final String CLAIMS_ID = ConservedHeader.CLAIMS_ID.getHeaderName();

    @Inject
    JAXRSLoopbackRequest request;

    // Conserved if supplied
    @Test
    public void conserveOtClaims() {
        final String claimsId = UUID.randomUUID().toString();
        String response = request.of("/conservedclaims")
        .request()
                .header(CLAIMS_ID, claimsId).get(String.class);
        Assert.assertEquals("foo", response);
    }

}
