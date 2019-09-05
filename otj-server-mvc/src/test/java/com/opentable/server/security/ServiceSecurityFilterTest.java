package com.opentable.server.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.opentable.httpheaders.OTHeaders;
import com.opentable.server.utils.ApplicationLogInMemoryAppender;
import com.opentable.servicesecurity.api.interfaces.operations.service.JWTHttpHeader;
import com.opentable.servicesecurity.servlet.ServiceSecurityFilter;

/**
 * Tests for ServiceSecurityFilter
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        TestServerWithKeys.class,
        TestSignerConfiguration.class
}, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
        "info.component=test"
})
public class ServiceSecurityFilterTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private TestSigner testSigner;

    @Test
    public void testApiCall_ValidToken() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityFilter.class);

        // generate valid unexpired token
        JWTHttpHeader jwtHttpHeader = testSigner.sign(false).get();
        String headerValue = JWTHttpHeader.toOTClaimsHeader(jwtHttpHeader);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OTHeaders.CLAIMS_ID, headerValue);
        String res = testRestTemplate.exchange("/api/test", HttpMethod.GET, new HttpEntity<>(map), String.class).getBody();
        assertEquals("test response", res);

        List<ObjectNode> events = inMemoryAppender.getEvents();
        assertNotNull(events);
        Optional<ObjectNode> logOpt = events.stream().filter(o -> o.get("message").asText().equals("Logging service security results")).findFirst();
        assertTrue(logOpt.isPresent());
        ObjectNode log = logOpt.get();

        assertEquals("SUCCESS", log.get("srvc-security-verification-status").asText());
        assertEquals("AUTHENTICATED", log.get("srvc-security-auth-status").asText());

        assertEquals("12345", log.get("srvc-security-identity").asText());
        assertEquals("USER", log.get("srvc-security-principal-type").asText());
        assertEquals("GPID", log.get("srvc-security-id-type").asText());
    }

    @Test
    public void testApiCall_ExpiredToken() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityFilter.class);

        // generate valid expired token
        JWTHttpHeader jwtHttpHeader = testSigner.sign(true).get();
        String headerValue = JWTHttpHeader.toOTClaimsHeader(jwtHttpHeader);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OTHeaders.CLAIMS_ID, headerValue);
        String res = testRestTemplate.exchange("/api/test", HttpMethod.GET, new HttpEntity<>(map), String.class).getBody();
        assertEquals("test response", res);

        List<ObjectNode> events = inMemoryAppender.getEvents();
        assertNotNull(events);
        Optional<ObjectNode> logOpt = events.stream().filter(o -> o.get("message").asText().equals("Logging service security results")).findFirst();
        assertTrue(logOpt.isPresent());
        ObjectNode log = logOpt.get();

        assertEquals("FAILED_EXPIRED", log.get("srvc-security-verification-status").asText());
        assertEquals("UNKNOWN", log.get("srvc-security-auth-status").asText());

        assertEquals("12345", log.get("srvc-security-identity").asText());
        assertEquals("USER", log.get("srvc-security-principal-type").asText());
        assertEquals("GPID", log.get("srvc-security-id-type").asText());
    }

    @Test
    public void testApiCall_NoHeader() throws NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityFilter.class);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        String res = testRestTemplate.exchange("/api/test", HttpMethod.GET, new HttpEntity<>(map), String.class).getBody();
        assertEquals("test response", res);

        List<ObjectNode> events = inMemoryAppender.getEvents();
        assertNotNull(events);
        Optional<ObjectNode> logOpt = events.stream().filter(o -> o.get("message").asText().equals("Logging service security results")).findFirst();
        assertTrue(logOpt.isPresent());
        ObjectNode log = logOpt.get();

        assertEquals("FAILED_EMPTY", log.get("srvc-security-verification-status").asText());
        assertEquals("UNKNOWN", log.get("srvc-security-auth-status").asText());

        assertNull(log.get("srvc-security-identity"));
        assertNull(log.get("srvc-security-principal-type"));
        assertNull(log.get("srvc-security-id-type"));
    }

    @Test
    public void testApiCall_EmptyHeader() throws NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityFilter.class);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OTHeaders.CLAIMS_ID, "");
        String res = testRestTemplate.exchange("/api/test", HttpMethod.GET, new HttpEntity<>(map), String.class).getBody();
        assertEquals("test response", res);

        List<ObjectNode> events = inMemoryAppender.getEvents();
        assertNotNull(events);
        Optional<ObjectNode> logOpt = events.stream().filter(o -> o.get("message").asText().equals("Logging service security results")).findFirst();
        assertTrue(logOpt.isPresent());
        ObjectNode log = logOpt.get();

        assertEquals("FAILED_EMPTY", log.get("srvc-security-verification-status").asText());
        assertEquals("UNKNOWN", log.get("srvc-security-auth-status").asText());

        assertNull(log.get("srvc-security-identity"));
        assertNull(log.get("srvc-security-principal-type"));
        assertNull(log.get("srvc-security-id-type"));
    }

    @Test
    public void testApiCall_MalformedHeader() throws NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityFilter.class);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OTHeaders.CLAIMS_ID, "token_without_leading_index");
        String res = testRestTemplate.exchange("/api/test", HttpMethod.GET, new HttpEntity<>(map), String.class).getBody();
        assertEquals("test response", res);

        List<ObjectNode> events = inMemoryAppender.getEvents();
        assertNotNull(events);
        Optional<ObjectNode> logOpt = events.stream().filter(o -> o.get("message").asText().equals("Logging service security results")).findFirst();
        assertTrue(logOpt.isPresent());
        ObjectNode log = logOpt.get();

        assertEquals("FAILED_EMPTY", log.get("srvc-security-verification-status").asText());
        assertEquals("UNKNOWN", log.get("srvc-security-auth-status").asText());

        assertNull(log.get("srvc-security-identity"));
        assertNull(log.get("srvc-security-principal-type"));
        assertNull(log.get("srvc-security-id-type"));
    }

    @Test
    public void testApiCall_BadKeyIndex() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityFilter.class);

        // generate valid unexpired token
        JWTHttpHeader jwtHttpHeader = testSigner.sign(false).get();
        assertNotNull(jwtHttpHeader);

        // set wrong key index
        String headerValue = "2 " + jwtHttpHeader.getEncodedJWt();

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OTHeaders.CLAIMS_ID, headerValue);
        String res = testRestTemplate.exchange("/api/test", HttpMethod.GET, new HttpEntity<>(map), String.class).getBody();
        assertEquals("test response", res);

        List<ObjectNode> events = inMemoryAppender.getEvents();
        assertNotNull(events);
        Optional<ObjectNode> logOpt = events.stream().filter(o -> o.get("message").asText().equals("Logging service security results")).findFirst();
        assertTrue(logOpt.isPresent());
        ObjectNode log = logOpt.get();

        assertEquals("FAILED_NO_PUBLIC_KEYS", log.get("srvc-security-verification-status").asText());
        assertEquals("UNKNOWN", log.get("srvc-security-auth-status").asText());

        assertNull(log.get("srvc-security-identity"));
        assertNull(log.get("srvc-security-principal-type"));
        assertNull(log.get("srvc-security-id-type"));
    }

    @Test
    public void testApiCall_BadToken() throws NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityFilter.class);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add(OTHeaders.CLAIMS_ID, "1 bad_token");
        String res = testRestTemplate.exchange("/api/test", HttpMethod.GET, new HttpEntity<>(map), String.class).getBody();
        assertEquals("test response", res);

        List<ObjectNode> events = inMemoryAppender.getEvents();
        assertNotNull(events);
        Optional<ObjectNode> logOpt = events.stream().filter(o -> o.get("message").asText().equals("Logging service security results")).findFirst();
        assertTrue(logOpt.isPresent());
        ObjectNode log = logOpt.get();

        assertEquals("FAILED_PARSE", log.get("srvc-security-verification-status").asText());
        assertEquals("UNKNOWN", log.get("srvc-security-auth-status").asText());

        assertNull(log.get("srvc-security-identity"));
        assertNull(log.get("srvc-security-principal-type"));
        assertNull(log.get("srvc-security-id-type"));
    }
}
