package com.opentable.server;

import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Test
    public void testEvents() {
        System.err.println("Waiting and chilling.");
    }
}
