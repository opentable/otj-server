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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
    "ot.httpserver.max-threads=" + AsyncBaseTest.N_THREADS,
    "jaxrs.client.default.connectionPoolSize=128"
})
public abstract class AsyncBaseTest {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncBaseTest.class);

    static final int N_THREADS = 16;
    static final int N_REQUESTS = 50;

    @Inject
    LoopbackRequest request;

    protected abstract EmbeddedJettyBase getEmbeddedJetty();

    @Test(timeout=30_000)
    public void testAsynchronousServerAndClient() {
        final EmbeddedJettyBase ej = getEmbeddedJetty();
        assertEquals(N_THREADS, ej.getThreadPool().getMaxThreads());

        IntStream.range(0, N_REQUESTS)
            .mapToObj(this::makeRequest)
            // Collect makes sure we send off all the requests before waiting for any
            .collect(Collectors.toList())
            .stream()
            .map(this::getSafe)
            .forEach(s -> {
                LOG.info("result received");
                assertEquals(TestServer.ASYNC_JOIN_RESULT, s);
            });
    }

    private Future<String> makeRequest(int i) {
        LOG.info("Submitting {}", i);
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
        return request.of("/async_join").queryParam("n", N_REQUESTS).queryParam("i", i).request().async().get(String.class);
    }

    private <T> T getSafe(Future<T> f) {
        try {
            return f.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        } catch (ExecutionException | TimeoutException e) {
            throw new AssertionError(e);
        }
    }
}
