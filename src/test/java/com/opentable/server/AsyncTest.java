package com.opentable.server;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.concurrent.OTExecutors;
import com.opentable.concurrent.TerminatingExecutorService;
import com.opentable.concurrent.ThreadPoolBuilder;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class
})
@TestPropertySource(properties = {
    "ot.httpserver.max-threads=" + AsyncTest.N_THREADS,
})
public class AsyncTest {
    static final int N_THREADS = 8;
    static final int N_REQUESTS = 10;

    @Inject
    EmbeddedJetty ej;

    @Inject
    LoopbackRequest request;

    @Test(timeout=20_000)
    public void testAsynchronousEndpoint() throws Exception {
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
