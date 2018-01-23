package com.opentable.server;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Inject;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class,
    EmbeddedReactiveJetty.class
})
@TestPropertySource(properties = {
    "ot.httpserver.max-threads=" + AsyncReactiveTest.N_THREADS,
})
public class AsyncReactiveTest extends AsyncBaseTest {

    @Inject
    EmbeddedReactiveJetty ejr;

    @Test(timeout=20_000)
    public void testAsynchronousEndpointUsingreactiveJetty() throws Exception {
        super.testAsynchronousEndpoint(ejr);
    }

}
