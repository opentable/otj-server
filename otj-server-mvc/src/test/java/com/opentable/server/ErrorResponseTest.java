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

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.opentable.conservedheaders.ConservedHeader;

// Basically calls an endpoint with all major methods, and shows they work
public class ErrorResponseTest extends AbstractTest {

    private static final String REQUEST_ID = ConservedHeader.REQUEST_ID.getHeaderKey();

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void get() {
        harness(HttpMethod.GET);
    }

    @Test
    public void post() {
        harness(HttpMethod.POST);
    }

    @Test
    public void put() {
        harness(HttpMethod.PUT);
    }

    @Test
    public void delete() {
        harness(HttpMethod.DELETE);
    }

    @Test
    public void options() {
        harness(HttpMethod.OPTIONS);
    }

    @Test
    @Ignore("HttpUrlConnection, default option in resttemplate doesn't support patch")
    public void patch() {
        harness(HttpMethod.PATCH);
    }

    public void harness(HttpMethod method) {
        // requestId in == the one out
        final String requestId = UUID.randomUUID().toString();
        ResponseEntity<JsonNode> response = request("/api/faultstatus-exception", method, REQUEST_ID, requestId);
        Assert.assertEquals(requestId, response.getHeaders().get(REQUEST_ID).get(0));
        Assert.assertEquals("I'm a teapot", response.getBody().at("/error").asText());
        Assert.assertEquals("unauthorized", response.getBody().at("/message").asText());
        Assert.assertEquals("418", response.getBody().at("/status").asText());
    }


    private ResponseEntity<JsonNode> request(String url, HttpMethod method, String header, String value) {
        HttpHeaders headers = new HttpHeaders();
        if (header != null) {
            headers.add(header, value);
        }
        return testRestTemplate.exchange(url,
            method,
            new HttpEntity<>(null, headers),
            JsonNode.class);
    }

}
