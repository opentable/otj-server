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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.opentable.conservedheaders.ConservedHeader;

public class ConservedHeadersTest extends AbstractTest {
    private final static String RID = ConservedHeader.REQUEST_ID.getHeaderName();
    private final static String AID = ConservedHeader.ANONYMOUS_ID.getHeaderName();

    @Test
    public void createRequestIdIndex() {
        sanityCheck(request("/api/test", null, null));
    }

    @Test
    public void createRequestId404() {
        sanityCheck(request("/404/not/found", null, null));
    }

    @Test
    public void conserveRequestId() {
        final String rid = UUID.randomUUID().toString();
        ResponseEntity<String> resp = request("/api/test", RID, rid);
        sanityCheck(resp);
        Assert.assertEquals(rid, resp.getHeaders().get(RID).get(0));
    }

    @Test
    public void replaceBadRequestId() {
        final String badRid = "not a valid UUID";
        ResponseEntity<String> resp = request("/api/test", RID, badRid);
        sanityCheck(resp);
        Assert.assertNotEquals(badRid, resp.getHeaders().get(RID).get(0));
    }

    @Test
    public void conserveAnonymousId() {
        final String aid = "fgsfds";
        ResponseEntity<String> resp = request("/api/test", AID, aid);
        sanityCheck(resp);
        Assert.assertEquals(aid, resp.getHeaders().get(AID).get(0));

    }

    private ResponseEntity<String> request(String url, String header, String value) {
        HttpHeaders headers = new HttpHeaders();
        if (header != null) {
            headers.add(header, value);
        }
        return restTemplate.exchange("http://localhost:" + port + url,
            HttpMethod.GET,
            new HttpEntity<>(null, headers),
            String.class);

    }

    private void sanityCheck(final ResponseEntity<String> resp) {
        final HttpHeaders headers = resp.getHeaders();
        List<String> requestIds = headers.get(RID);
        Assert.assertNotNull(requestIds);
        Assert.assertEquals(requestIds.size(), 1);
        Assert.assertNotNull(requestIds.get(0));
    }
}
