package com.opentable.server;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestMvcServerConfiguration.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles(profiles = "test")
@TestPropertySource(properties = {
    "info.component=test",
    "ot.httpserver.active-connectors=boot"
})
@Import(RestTemplateAutoConfiguration.class)
public abstract class AbstractTest {

    RestTemplate restTemplate;
    HttpServerInfo httpServerInfo;

    @Value("${local.server.port}")
    int port;

    @Inject
    public void init(RestTemplateBuilder restTemplateBuilder, Provider<HttpServerInfo> info) {
        this.restTemplate = restTemplateBuilder.build();
        this.restTemplate.setErrorHandler(new ResponseErrorHandler() {

            @Override
            public boolean hasError(ClientHttpResponse clientHttpResponse) throws IOException {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse clientHttpResponse) throws IOException {

            }
        });
        this.httpServerInfo = info.get();
    }

}
