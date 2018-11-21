package com.opentable.server.tests;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;


import com.opentable.server.mvc.MVCServer;
import com.opentable.service.ServiceInfo;

@SpringBootApplication
@MVCServer
public class TestMvcServer2Configuration {

    WebApplicationContext context;

    @Bean
    ServiceInfo serviceInfo(@Value("${info.component:test-server}") final String serviceType) {
        return () -> serviceType;
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    public static void main(String[] args) {
        SpringApplication.run(TestMvcServer2Configuration.class, args);
    }

}
