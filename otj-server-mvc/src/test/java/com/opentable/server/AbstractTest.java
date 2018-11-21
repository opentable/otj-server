package com.opentable.server;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestMvcServerConfiguration.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
    "info.component=test",
    "ot.httpserver.active-connectors=boot"
})
public abstract class AbstractTest {

    HttpServerInfo httpServerInfo;

    @Value("${local.server.port}")
    int port;

    @Inject
    public void init(Provider<HttpServerInfo> info) {
        this.httpServerInfo = info.get();
    }

}
