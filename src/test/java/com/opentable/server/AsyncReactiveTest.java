package com.opentable.server;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class,
    EmbeddedReactiveJetty.class
})
@TestPropertySource(properties = {
    "ot.httpserver.max-threads=" + AsyncBaseTest.N_THREADS,
})
@Ignore
public class AsyncReactiveTest extends AsyncBaseTest {

    @Inject
    EmbeddedReactiveJetty ejr;

    @Override
    protected EmbeddedJettyBase getEmbeddedJetty() {
        return ejr;
    }
}
