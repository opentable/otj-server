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
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.conservedheaders.ConservedHeader;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestMvcServerConfiguration.class, OverrideConfiguration.class
}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
        "info.component=test",
        "ot.httpserver.active-connectors=boot",
        "OT-Claims=foo",
})
@DirtiesContext
public class OverrideConservedHeadersTest {

    HttpServerInfo httpServerInfo;

    @Value("${local.server.port}")
    int port;

    @Inject
    public void init(Provider<HttpServerInfo> info) {
        this.httpServerInfo = info.get();
    }

    private static final String CLAIMS_ID = ConservedHeader.CLAIMS_ID.getHeaderName();

    @Autowired
    private TestRestTemplate testRestTemplate;


    // Conserved if supplied
    @Test
    public void conserveOtClaims() {
        final String claimsId = UUID.randomUUID().toString();
        ResponseEntity<String> response = request("/api/conservedclaims", CLAIMS_ID, claimsId);
        Assert.assertEquals("foo", response.getBody());
    }

    private ResponseEntity<String> request(String url, String header, String value) {
        HttpHeaders headers = new HttpHeaders();
        if (header != null) {
            headers.add(header, value);
        }
        return testRestTemplate.exchange(url,
            HttpMethod.GET,
            new HttpEntity<>(null, headers),
            String.class);

    }
}
