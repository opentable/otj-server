package com.opentable.server;

import com.opentable.concurrent.OTExecutors;
import com.opentable.concurrent.TerminatingExecutorService;
import com.opentable.concurrent.ThreadPoolBuilder;
import org.springframework.test.context.TestPropertySource;
import javax.inject.Inject;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

@TestPropertySource(properties = {
    "ot.httpserver.max-threads=" + AsyncBaseTest.N_THREADS,
})
public abstract class AsyncBaseTest {
    static final int N_THREADS = 8;
    static final int N_REQUESTS = 10;

    @Inject
    LoopbackRequest request;

    public void testAsynchronousEndpoint(EmbeddedJettyBase ej) throws Exception {
        assertEquals(N_THREADS, ej.getThreadPool().getMaxThreads());

        try (TerminatingExecutorService es = OTExecutors.autoTerminate(
                ThreadPoolBuilder.longTaskPool("request", N_REQUESTS).build(), Duration.ofSeconds(1))) {
            IntStream.range(0, N_REQUESTS)
                .mapToObj(i -> es.submit(this::makeRequest))
                // Collect makes sure we send off all the requests before waiting for any
                .collect(Collectors.toList())
                .stream()
                .map(f -> getSafe(f))
                .forEach(s -> assertEquals(TestServer.ASYNC_JOIN_RESULT, s));
        }
    }
    private String makeRequest() {
        return request.of("/async_join").queryParam("n", N_REQUESTS).request().get(String.class);
    }

    private <T> T getSafe(Future<T> f) {
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        } catch (ExecutionException e) {
            throw new AssertionError(e);
        }
    }
}
