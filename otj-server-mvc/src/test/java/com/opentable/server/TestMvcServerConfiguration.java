package com.opentable.server;

import static org.assertj.core.api.Assertions.fail;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import com.opentable.service.ServiceInfo;

@Configuration
@Import(RestTemplateAutoConfiguration.class)
@MVCServer
public class TestMvcServerConfiguration {

    @Bean
    ServiceInfo serviceInfo(@Value("${info.component:test-server}") final String serviceType) {
        return () -> serviceType;
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
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
        public EchoResponse echo(@RequestHeader HttpHeaders headers) {
            return restTemplate.exchange("https://postman-echo.com/get",
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                EchoResponse.class).getBody();
        }

        @PostMapping("map")
        public @ResponseBody MrBean map(@RequestBody MrBean req) {
            return req;
        }

        @GetMapping("rsp")
        public @ResponseBody MrBean rsp() {
            return new MrBean("2", "1");
        }

        @GetMapping("async")
        public DeferredResult<String> async() {
            DeferredResult<String> output = new DeferredResult<>();

            ForkJoinPool.commonPool().submit(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    fail("sleep was interupted", e);
                }
                output.setResult("test");
            });

            return output;
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

    public static class MrBean {

        private final String foo, bar;

        public String getFoo() {
            return foo;
        }

        public String getBar() {
            return bar;
        }

        @JsonCreator
        public MrBean(String foo, String bar) {
            this.foo = foo;
            this.bar = bar;
        }
    }

}
