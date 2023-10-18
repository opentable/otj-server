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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import com.opentable.conservedheaders.ConservedHeader;
import com.opentable.service.ServiceInfo;

@Configuration
@ReactiveServer
public class TestReactiveServerConfiguration {

    @Bean
    ServiceInfo serviceInfo(@Value("${info.component:service-test}") final String serviceType) {
        return () -> serviceType;
    }

    @Bean
    WebClient webClient() {
        return WebClient.builder().build();
    }

    /**
     * This scheduler is used by {@link ConservedHeadersWebFilterTest}
     * to set up fewer available threads than the number of requests we want to make in parallel, to ensure that
     * reactive operations from different requests are scheduled on threads that were previously used by another
     * request.
     */
    @Bean("parallelScheduler")
    Scheduler getScheduler() {
        return Schedulers.newParallel("parallel-scheduler", 3);
    }

    @RestController
    @RequestMapping("/api")
    public static class MyResource {

        private static final Logger LOG = LoggerFactory.getLogger(MyResource.class);

        @Autowired
        private WebClient webClient;

        @Autowired
        @Qualifier("parallelScheduler")
        private Scheduler parallelScheduler;

        @RequestMapping("test")
        public Mono<String> test() {
            LOG.info("Called 'test' endpoint");
            return Mono.just("test");
        }

        @RequestMapping("host")
        public Mono<String> host(ServerHttpRequest serverHttpRequest) {
            return Mono.just(serverHttpRequest.getURI().toString());
        }

        @RequestMapping("conservedclaims")
        public Mono<String> cl() {
            LOG.info("Called 'test' endpoint");
            return Mono.just(MDC.get(ConservedHeader.CLAIMS_ID.getMDCKey()));
        }

        @GetMapping("echo")
        public Mono<EchoResponse> echo(@RequestHeader HttpHeaders headers) {
            return webClient.get()
                    .uri("https://postman-echo.com/get")
                    .headers(h -> h.addAll(headers))
                    .exchangeToMono(clientResponse -> clientResponse.bodyToMono(EchoResponse.class));
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
            return Mono.just("test").delayElement(Duration.ofMillis(500), Schedulers.boundedElastic());
        }

        @GetMapping("threading/{trace}")
        public Mono<String> getServiceLookup(@PathVariable("trace") String trace) throws Exception {

            LOG.info("(TRACE-{}) Before publisher chain, before sleep", trace);

            Thread.sleep(1000);

            LOG.info("(TRACE-{}) After sleep, before publisher chain", trace);

            return Mono.just("Just some random thing")
                    .map(c -> {
                        LOG.error("(TRACE-{}) Inside publisher chain, before error, {}", trace, c);
                        throw new RuntimeException("Throwing an error");
                    })
                    .onErrorResume(d -> {
                        LOG.error("(TRACE-{}) Inside publisher, after error", trace, d);
                        return Mono.just("Alternate random thing");
                    })
                    .flatMap(e -> {
                        LOG.info("(TRACE-{}) Inside publisher, before scheduling, {}", trace, e);
                        return Mono.just(e);
                    })
                    .publishOn(parallelScheduler)
                    .flatMap(f ->
                    {
                        LOG.info("(TRACE-{}) After scheduling, inside flatMap, before callable, {}", trace, f);
                        return Mono.fromCallable(() -> "Callable response")
                                .flatMap(g -> {
                                    LOG.info("(TRACE-{}) Inside flatMap, after callable, before scheduling #2, {}", trace, g);
                                    return Mono.just(g);
                                })
                                .publishOn(parallelScheduler)
                                .flatMap(h -> {
                                    LOG.info("(TRACE-{}) Inside flatpMap, after scheduling #2, {}", trace, h);
                                    return Mono.just(h);
                                });
                    });
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
