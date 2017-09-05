package com.opentable.server;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class, EventTestListener.class
})
@TestPropertySource(properties= {
    "ot.httpserver.max-threads=13",
})
public class EventTest {
    private static final Logger LOG = LoggerFactory.getLogger(EventTest.class);

    @Inject
    HttpServerInfo info;

    @Test
    public void testEvents() {

        LOG.debug("Waiting and chilling. " + info.getPort());
    }
}
