package com.opentable.server;

import javax.inject.Inject;
import org.junit.Test;
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
public class AsyncTest extends AsyncBaseTest{

    @Inject
    EmbeddedJetty ej;

    @Test(timeout=20_000)
    public void testAsynchronousEndpointUsingreactiveJetty() throws Exception {
        super.testAsynchronousEndpoint(ej);
    }
}
