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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.opentable.httpheaders.OTHeaders;
import com.opentable.logging.otl.HttpV1;
import com.opentable.server.reactive.utils.ApplicationLogInMemoryAppender;
import com.opentable.server.reactive.utils.RequestLogInMemoryAppender;

/**
 * Integration tests for {@link com.opentable.conservedheaders.reactive.ConservedHeadersWebFilter}.
 * <p>
 * Verifies the following:
 * <p>
 * For an incoming request containing conserved headers:
 * - Those headers are included on the outgoing response.
 * - If no request-id header is included, it is added in automatically.
 * - Log messages associated with the request contain the appropriate conserved header values.
 * - When reactive operations move between threads (see {@link TestReactiveServerConfiguration} /api/threading)
 * conserved header information continues to be associated with the request.
 * - When multiple requests are in-flight simultaneously, conserved header information is associated with the
 * correct request, even when reactive operations are continued on a thread previously used by a different request.
 */
public class ConservedHeadersWebFilterTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConservedHeadersWebFilterTest.class);

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testApiCallConservesHeadersOnResponse() {
        final String requestId = UUID.randomUUID().toString();
        final String anonId = UUID.randomUUID().toString();

        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .header(OTHeaders.REQUEST_ID, requestId)
                .header(OTHeaders.ANONYMOUS_ID, anonId)
                .header("Not-A-Conserved-Header", "some value")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(OTHeaders.REQUEST_ID, requestId)
                .expectHeader().valueEquals(OTHeaders.ANONYMOUS_ID, anonId)
                .expectHeader().doesNotExist("Not-A-Conserved-Header")
                .expectBody(String.class)
                .returnResult();

        final String res = result.getResponseBody();
        assertEquals("test", res);
    }

    @Test
    public void testApiCallWithNoRequestIdAddsARequestId() {
        EntityExchangeResult<String> result = webTestClient.get()
                .uri("/api/test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists(OTHeaders.REQUEST_ID)
                .expectBody(String.class)
                .returnResult();

        final String res = result.getResponseBody();
        assertEquals("test", res);
    }

    /**
     * This test calls an endpoint on {@link TestReactiveServerConfiguration} a random number of times in parallel
     * while collecting the application and request logs for all n requests in the two in-memory appenders.
     * <p>
     * The API request, /api/threading/{trace}, does the following:
     * - For the given reactive request, performs a number of reactive operations in a reactive chain.
     * - For each reactive operation, emits an application log message containing the {trace} parameter that was passed in.
     * - Occasionally schedules operations in that reactive chain on different threads using a shared scheduler.
     * <p>
     * Each request contains a unique request-id and anonymous-id, which should be associated with the
     * application and request log messages for that request, via the MDC.
     * <p>
     * When each parallel request completes, we collect the unique request-id, anonymous-id and trace results
     * and do the following:
     * - Filter messages in the appender to only those containing the trace id of the request.
     * - Verifies that each application message contains the correct request-id and anonymous-id.
     * - Verifies that each request message contains the correct request-id and anonymous-id.
     * <p>
     * Furthermore, the shared scheduler (see {@link TestReactiveServerConfiguration}) contains fewer available
     * threads than the number of parallel requests invoked in the ForkJoinPool. This forces reactive operations
     * to be scheduled on threads that were previously used to handle parts of a different request, thus
     * ensuring that the MDC conserved header values are set correctly even if the thread was re-used.
     */
    @Test
    public void testParallelApiCallsLogConservedHeadersInMDCForCorrectRequest() throws NoSuchFieldException, IllegalAccessException {

        final ApplicationLogInMemoryAppender appLogAppender = ApplicationLogInMemoryAppender.create(TestReactiveServerConfiguration.MyResource.class);
        final RequestLogInMemoryAppender requestLogAppender = RequestLogInMemoryAppender.create();

        // Create 5-10 random API calls
        Random r = new Random();
        int parallelRequests = r.nextInt((10 - 5) + 1) + 5;

        List<TestApiCall> apiCalls = new ArrayList<>(parallelRequests);
        for (int i = 1; i <= parallelRequests; i++) {
            TestApiCall apiCall = new TestApiCall(webTestClient, Integer.toString(i),
                    // Request-id should always be provided, or else it will be auto-generated and then we can't test for it
                    UUID.randomUUID().toString(),
                    // Randomly decide to provide the remaining headers or not
                    randomUUIDorNull(i),
                    randomUUIDorNull(i)
            );
            LOG.info("Created test api call: {}", apiCall);
            apiCalls.add(apiCall);
        }

        // Shuffle the list of api calls so we aren't calling it in the same order every time
        Collections.shuffle(apiCalls);

        final ForkJoinPool forkJoinPool = new ForkJoinPool(parallelRequests);
        forkJoinPool.invokeAll(apiCalls).forEach(f -> {
            LoggingTestResult tryResult = null;
            try {
                tryResult = f.get();
            } catch (Exception e) {
                fail("Got exception: " + e.getMessage());
            }
            assertNotNull(tryResult);

            final LoggingTestResult result = tryResult;

            final List<ObjectNode> appLogs = appLogAppender.getEvents().stream()
                    .filter(e -> e.get("message").textValue().startsWith("(TRACE-" + result.trace + ")"))
                    .collect(Collectors.toList());

            assertEquals(8, appLogs.size());
            appLogs.forEach(appLog -> {
                LOG.info("Got application log with thread-name [{}], request-id [{}], anonymous-id [{}]: {}",
                        appLog.get("thread-name"), appLog.get("request-id"), appLog.get("anonymous-id"), appLog.get("message"));

                verifyApplicationHeader(result.trace, appLog, "request-id", result.requestId);
                verifyApplicationHeader(result.trace, appLog, "anonymous-id", result.anonId);
                verifyApplicationHeader(result.trace, appLog, "session-id", result.sessionId);
            });

            final List<HttpV1> requestLogs = requestLogAppender.getEvents().stream()
                    .filter(e -> e.getUrl().equals("/api/threading/" + result.trace))
                    .collect(Collectors.toList());

            assertEquals(1, requestLogs.size());
            requestLogs.forEach(e -> {
                LOG.info("Got request log with request-id [{}], anonymous-id [{}]: {}",
                        e.getRequestId(), e.getAnonymousId(), e.getUrl());

                assertEquals("request log request-id should match", result.requestId, e.getRequestId().toString());
                assertEquals("request log anonymous-id should match", result.anonId, e.getAnonymousId());
                assertEquals("request log session-id should match", result.sessionId, e.getSessionId());
            });

        });
    }

    // Random chance of returning a UUID for a header value or null
    private String randomUUIDorNull(int seed) {
        Random r = new Random(seed);
        int i = r.nextInt(100);
        if (i % 2 != 0) {
            return UUID.randomUUID().toString();
        } else {
            return null;
        }
    }

    private void verifyApplicationHeader(String trace, ObjectNode appLog, String headerName, String expectedValue) {
        JsonNode headerNode = appLog.get(headerName);
        if (expectedValue != null && headerNode != null) {
            assertEquals("application log header should match for " + headerName + " with TRACE-" + trace,
                    expectedValue, headerNode.textValue());
        } else {
            assertNull(expectedValue);
            assertNull("Expected null header for TRACE-" + trace, headerNode);
        }
    }

    private static class TestApiCall implements Callable<LoggingTestResult> {

        private final WebTestClient webTestClient;
        private final String trace;
        private final String requestId;
        private final String anonId;
        private final String sessionId;

        public TestApiCall(WebTestClient webTestClient, String trace,
                           String requestId,
                           String anonId,
                           String sessionId) {
            this.webTestClient = webTestClient;
            this.trace = trace;
            this.requestId = requestId;
            this.anonId = anonId;
            this.sessionId = sessionId;
        }

        @Override
        public LoggingTestResult call() throws Exception {

            WebTestClient.RequestHeadersSpec<?> spec = webTestClient.get()
                    .uri("/api/threading/{trace}", trace);

            spec = addHeaderToSpec(OTHeaders.REQUEST_ID, requestId, spec);
            spec = addHeaderToSpec(OTHeaders.ANONYMOUS_ID, anonId, spec);
            spec = addHeaderToSpec(OTHeaders.SESSION_ID, sessionId, spec);

            EntityExchangeResult<String> result = spec.exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult();

            final String res = result.getResponseBody();
            assertEquals("Callable response", res);

            Thread.sleep(2000L);

            return new LoggingTestResult(trace, requestId, anonId, sessionId);
        }

        private WebTestClient.RequestHeadersSpec addHeaderToSpec(String headerName, String headerValue,
                                                                 WebTestClient.RequestHeadersSpec spec) {
            if (headerValue != null) {
                spec = spec.header(headerName, headerValue);
            }
            return spec;
        }

        @Override
        public String toString() {
            return "TestApiCall{" +
                    "trace='" + trace + '\'' +
                    ", requestId='" + requestId + '\'' +
                    ", anonId='" + anonId + '\'' +
                    ", sessionId='" + sessionId + '\'' +
                    '}';
        }
    }

    private static class LoggingTestResult {
        final String trace;
        final String requestId;
        final String anonId;
        final String sessionId;

        public LoggingTestResult(String trace, String requestId, String anonId, String sessionId) {
            this.trace = trace;
            this.requestId = requestId;
            this.anonId = anonId;
            this.sessionId = sessionId;
        }
    }

}
