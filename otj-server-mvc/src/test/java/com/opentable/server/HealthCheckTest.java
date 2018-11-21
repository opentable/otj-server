package com.opentable.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HealthCheckTest extends AbstractTest {

    @Autowired
    public TestRestTemplate testRestTemplate;

    @Test
    public void testHealthEndpoint() {
        ResponseEntity<String> healthResponse = testRestTemplate.getForEntity("/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testServiceStatusEndpoint() {
        ResponseEntity<String> healthResponse = testRestTemplate.getForEntity("/service-status", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}

