/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.opentable.server.reactive;

import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import com.opentable.service.ServiceInfo;

/**
 *
 */
@Configuration
@ReactiveServer
public class TestReactiveServerConfiguration {

    public static void main(String[] args) {
        SpringApplication.run(TestReactiveServerConfiguration.class, args);
    }

    @Bean
    ServiceInfo serviceInfo(@Value("${info.component:test-service}") final String serviceType) {
        return () -> serviceType;
    }

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }

    @RestController
    @RequestMapping("/api")
    public static class MyResource {

        @Autowired
        private WebClient webClient;

        @RequestMapping("test")
        public Mono<String> test() {
            return Mono.just("test");
        }

        @GetMapping("echo")
        public Mono<EchoResponse> echo(@RequestHeader HttpHeaders headers) {
            return webClient.get()
                    .uri("https://postman-echo.com/get")
                    .headers(h -> h.addAll(headers))
                    .exchange()
                    .flatMap(clientResponse -> clientResponse.bodyToMono(EchoResponse.class));
        }

        @GetMapping("fault")
        public void rspFault() {
            throw new RuntimeException("test");
        }

        @GetMapping("fault2")
        public void rspFault2() {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "test specific error");
        }

        @GetMapping("async")
        public Mono<String> async() {
            return Mono.just("test").delayElement(Duration.ofMillis(500), Schedulers.elastic());
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
