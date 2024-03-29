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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.opentable.conservedheaders.ConservedHeader;
import com.opentable.server.TestMvcServerConfiguration.EchoResponse;
// Very similar to jaxrs
public class ConservedHeadersTest extends AbstractTest {

    private static final String REQUEST_ID = ConservedHeader.REQUEST_ID.getHeaderKey();
    private static final String CLAIMS_ID = ConservedHeader.CLAIMS_ID.getHeaderKey();
    private static final String ANONYMOUS_ID = ConservedHeader.ANONYMOUS_ID.getHeaderKey();

    @Autowired
    private TestRestTemplate testRestTemplate;

    // New Id
    @Test
    public void createRequestIdIndex() {
        sanityCheck(request("/api/test", null, null));
    }

    // New Id
    @Test
    public void createRequestId404() {
        sanityCheck(request("/404/not/found", null, null));
    }

    // Conserved if supplied
    @Test
    public void conserveRequestId() {
        final String requestId = UUID.randomUUID().toString();
        ResponseEntity<String> response = request("/api/test", REQUEST_ID, requestId);
        sanityCheck(response);
        Assert.assertEquals(requestId, response.getHeaders().get(REQUEST_ID).get(0));
    }

    // Conserved if supplied
    @Test
    public void conserveOtClaims() {
        final String claimsId = UUID.randomUUID().toString();
        ResponseEntity<String> response = request("/api/conservedclaims", CLAIMS_ID, claimsId);
        Assert.assertEquals(claimsId, response.getBody());
    }

    // Conserved in async
    @Test
    public void conserveRequestIdInAsync() {
        final String requestId = UUID.randomUUID().toString();
        ResponseEntity<String> response = request("/api/async", REQUEST_ID, requestId);
        sanityCheck(response);
        Assert.assertEquals(requestId, response.getHeaders().get(REQUEST_ID).get(0));
    }

    // Bad id replaced
    @Test
    public void replaceBadRequestId() {
        final String badRequestId = "not a valid UUID";
        ResponseEntity<String> response = request("/api/test", REQUEST_ID, badRequestId);
        sanityCheck(response);
        Assert.assertNotEquals(badRequestId, response.getHeaders().get(REQUEST_ID).get(0));
    }

    // Same test for anonymous id
    @Test
    public void conserveAnonymousId() {
        final String anonymousId = "fgsfds";
        ResponseEntity<String> response = request("/api/test", ANONYMOUS_ID, anonymousId);
        sanityCheck(response);
        Assert.assertEquals(anonymousId, response.getHeaders().get(ANONYMOUS_ID).get(0));

    }

    // into the server, out via response
    @Test
    public void testRequestIdPassedAlong() {
        String requestId = UUID.randomUUID().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.add(REQUEST_ID, requestId);
        EchoResponse echoResponse = testRestTemplate.exchange("/api/echo",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                EchoResponse.class).getBody();
        System.out.println(echoResponse);
        String actualRequestId = echoResponse.headers.get("ot-requestid");
        assertEquals(requestId, actualRequestId);
    }

    // Even for an exception
    @Test
    public void conserveRequestIdFault() {
        final String requestId = UUID.randomUUID().toString();
        ResponseEntity<String> response = request("/api/fault", REQUEST_ID, requestId);
        sanityCheck(response);
        Assert.assertEquals(requestId, response.getHeaders().get(REQUEST_ID).get(0));
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

    private void sanityCheck(final ResponseEntity<String> resp) {
        final HttpHeaders headers = resp.getHeaders();
        List<String> requestIds = headers.get(REQUEST_ID);
        Assert.assertNotNull(requestIds);
        Assert.assertEquals(requestIds.size(), 1);
        Assert.assertNotNull(requestIds.get(0));
    }
}
