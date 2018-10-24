package com.opentable.server;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.opentable.resttemplate.RestTemplateConfiguration;
import com.opentable.resttemplate.RestTemplateFactory;
import com.opentable.server.TestMvcServerConfiguration.MyResource;
import com.opentable.service.ServiceInfo;
import com.opentable.service.discovery.client.EnableDiscoveryClient;

@Configuration
@Import({RestTemplateConfiguration.class, MyResource.class})
@EnableDiscoveryClient
@MVCServer
public class TestMvcServerConfiguration {

    @Bean
    ServiceInfo serviceInfo(@Value("${info.component}") final String serviceType) {
        return () -> serviceType;
    }

    @Bean
    RestTemplate restTemplate(RestTemplateFactory factory) {
        return factory.newTemplate();
    }

    public static void main(String[] args) {
        SpringApplication.run(TestMvcServerConfiguration.class, args);
    }

    @RestController
    @RequestMapping("/api")
    public static class MyResource {

        @Autowired private RestTemplate restTemplate;

        @RequestMapping("test")
        public String test() {
            return "test";
        }

        @GetMapping("echo")
        public EchoResponse echo() {
            return restTemplate.getForEntity("https://postman-echo.com/get", EchoResponse.class).getBody();
        }
    }

    public static class EchoResponse {
        @JsonProperty("args")
        Map<String, String> args;

        @JsonProperty("headers")
        Map<String, String> headers;

        public EchoResponse() {

        }

        @Override
        public String toString() {
            return "EchoResponse [args=" + args + ", headers=" + headers + "]";
        }

    }

}
