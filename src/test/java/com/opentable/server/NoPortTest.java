package com.opentable.server;

import java.util.Collections;

import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextException;

public class NoPortTest {
    @Test(expected=ApplicationContextException.class)
    public void testNoPorts() throws Exception {
        SpringApplication app = new SpringApplication(TestServer.class);
        app.setDefaultProperties(Collections.singletonMap("ot.http.bind-port", ""));
        app.run();
    }
}
