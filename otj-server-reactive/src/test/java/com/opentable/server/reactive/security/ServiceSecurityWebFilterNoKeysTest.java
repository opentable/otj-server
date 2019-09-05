package com.opentable.server.reactive.security;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.opentable.httpheaders.OTHeaders;
import com.opentable.server.reactive.TestReactiveServerConfiguration;
import com.opentable.server.reactive.utils.ApplicationLogInMemoryAppender;
import com.opentable.servicesecurity.api.interfaces.operations.service.JWTHttpHeader;
import com.opentable.servicesecurity.reactive.ServiceSecurityWebFilter;

/**
 * Tests for {@link ServiceSecurityWebFilter} when no public keys are available in the application context
 * (i.e. the service is not integrated with Credentials Management).
 *
 * We want the service to continue to be operable after installing the filter, but verification steps will
 * intentionally fail (and the request will continue to be processed).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        TestReactiveServerConfiguration.class,
        TestSignerConfiguration.class
} , webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
        "info.component=test",
})
public class ServiceSecurityWebFilterNoKeysTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestSigner testSigner;

    @Test
    public void testApiCall_ValidToken() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityWebFilter.class);

        // generate valid unexpired token
        JWTHttpHeader jwtHttpHeader = testSigner.sign(false).get();
        String headerValue = JWTHttpHeader.toOTClaimsHeader(jwtHttpHeader);

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .header(OTHeaders.CLAIMS_ID, headerValue)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);

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
    public void testApiCall_ExpiredToken() throws ExecutionException, InterruptedException, NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityWebFilter.class);

        // generate valid expired token
        JWTHttpHeader jwtHttpHeader = testSigner.sign(true).get();
        String headerValue = JWTHttpHeader.toOTClaimsHeader(jwtHttpHeader);

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .header(OTHeaders.CLAIMS_ID, headerValue)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);

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
    public void testApiCall_NoHeader() throws NoSuchFieldException, IllegalAccessException {
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityWebFilter.class);

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);

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
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityWebFilter.class);

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .header(OTHeaders.CLAIMS_ID, "")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);

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
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityWebFilter.class);

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .header(OTHeaders.CLAIMS_ID, "token_without_leading_index")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);

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
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityWebFilter.class);

        // generate valid unexpired token
        JWTHttpHeader jwtHttpHeader = testSigner.sign(false).get();
        assertNotNull(jwtHttpHeader);

        // set wrong key index
        String headerValue = "2 " + jwtHttpHeader.getEncodedJWt();

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .header(OTHeaders.CLAIMS_ID, headerValue)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);

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
        ApplicationLogInMemoryAppender inMemoryAppender = ApplicationLogInMemoryAppender.create(ServiceSecurityWebFilter.class);

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .header(OTHeaders.CLAIMS_ID, "1 bad_token")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult();

        String res = result.getResponseBody();
        assertEquals("test", res);

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
}
