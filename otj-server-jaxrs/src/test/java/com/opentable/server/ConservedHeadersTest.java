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

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.conservedheaders.ConservedHeader;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestJaxRsServerConfiguration.class
})
public class ConservedHeadersTest {
    private final static String RID = ConservedHeader.REQUEST_ID.getHeaderName();
    private final static String AID = ConservedHeader.ANONYMOUS_ID.getHeaderName();
    private static final String CLAIMS_ID = ConservedHeader.CLAIMS_ID.getHeaderName();

    @Inject
    JAXRSLoopbackRequest request;

    // RequestId, even if not supplied is created AND returned in response
    @Test
    public void createRequestIdIndex() {
        sanityCheck(request.of("/").request().get());
    }

    @Test
    public void createRequestId404() {
        sanityCheck(request.of("/404/not/found").request().get());
    }

    // Here we prove if we supplied it on the call, it's the one that came in on the response
    @Test
    public void conserveRequestId() {
        final String rid = UUID.randomUUID().toString();
        try(final Response resp = request.of("/").request().header(RID, rid).get()){
            sanityCheck(resp);
            Assert.assertEquals(rid, resp.getHeaderString(RID));
        }
    }

    // Conserved if supplied
    @Test
    public void conserveOtClaims() {
        final String claimsId = UUID.randomUUID().toString();
        String response = request.of("/conservedclaims").request().header(CLAIMS_ID, claimsId).get(String.class);
        Assert.assertEquals(claimsId, response);
    }

    @Test
    public void conserveRequestIdWithFault() {
        final String rid = UUID.randomUUID().toString();
        try(final Response resp = request.of("/5xx").request().header(RID, rid).get()){
            sanityCheck(resp);
            Assert.assertEquals(rid, resp.getHeaderString(RID));
        }
    }

    @Test
    public void conserveRequestIdWithFault2() {
        final String rid = UUID.randomUUID().toString();
        try(final Response resp = request.of("/5xx2").request().header(RID, rid).get()){
            sanityCheck(resp);
            Assert.assertEquals(rid, resp.getHeaderString(RID));
        }
    }

    // Bad requestId are discarded and replaced with a fresh UUID
    @Test
    public void replaceBadRequestId() {
        final String badRid = "not a valid UUID";
        try(final Response resp = request.of("/").request().header(RID, badRid).get()){
            sanityCheck(resp);
            Assert.assertNotEquals(badRid, resp.getHeaderString(RID));
        }
    }

    // As with Request Id, so test anonymous id.
    @Test
    public void conserveAnonymousId() {
        final String aid = "fgsfds";
        try(final Response resp = request.of("/").request().header(AID, aid).get()){
            sanityCheck(resp);
            Assert.assertEquals(aid, resp.getHeaderString(AID));
        }
    }

    private void sanityCheck(final Response resp) {
        final MultivaluedMap<String, Object> headers = resp.getHeaders();
        final List<Object> requestIds = headers.get(RID);
        Assert.assertNotNull(requestIds);
        Assert.assertEquals(requestIds.size(), 1);
        Assert.assertNotNull(requestIds.get(0));
    }
}
