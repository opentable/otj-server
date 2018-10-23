package com.opentable.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= {TestMvcServerConfiguration.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
public class MVCServerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testConservedHeaders() {
        String body = this.restTemplate.getForObject("/api/test", String.class);
        assertThat(body).isEqualTo("test");
    }
}

