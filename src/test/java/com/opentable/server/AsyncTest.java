package com.opentable.server;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class
})
@Ignore
public class AsyncTest extends AsyncBaseTest {

    @Inject
    EmbeddedJetty ej;

    @Override
    protected EmbeddedJettyBase getEmbeddedJetty() {
        return ej;
    }
}
