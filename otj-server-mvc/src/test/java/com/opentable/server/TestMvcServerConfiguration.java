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
package com.opentable.server;

import static org.assertj.core.api.Assertions.fail;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import com.opentable.conservedheaders.ConservedHeader;
import com.opentable.server.mvc.MVCServer;
import com.opentable.service.ServiceInfo;

@Configuration
@Import(RestTemplateAutoConfiguration.class)
@MVCServer
public class TestMvcServerConfiguration {

    @Bean
    ServiceInfo serviceInfo(@Value("${info.component:test-service}") final String serviceType) {
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

        @GetMapping("host")
        public String host(HttpServletRequest httpServletRequest) {
            return httpServletRequest.getScheme() +"://" + httpServletRequest.getServerName();
        }
        @PostMapping("map")
        public @ResponseBody MrBean map(@RequestBody MrBean req) {
            return req;
        }

        @GetMapping("rsp")
        public @ResponseBody MrBean rsp() {
            return new MrBean("2", "1");
        }

        @GetMapping("request-param")
        public @ResponseBody String rspParams(
            @RequestParam(value = "date", required = true) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
        ) {
            return date.toString();
        }


        @GetMapping("conservedclaims")
        public ResponseEntity<String> conserved() {
            return ResponseEntity.ok(MDC.get(ConservedHeader.CLAIMS_ID.getMDCKey()));
        }
        @GetMapping("/nuclear-launch-codes")
        @RolesAllowed("POTUS") // This annotation doesn't work without spring security
        public ResponseEntity<String> getLaunchCode(HttpServletRequest request) {
            if (!request.isUserInRole("POTUS")) {
                return ResponseEntity.status(403).header("X-Role-Inferred", request.getUserPrincipal().getName() == null ? "none" : request.getUserPrincipal().getName()).build();
            }
            return ResponseEntity.status(200).header("X-Role-Inferred", request.getUserPrincipal().getName())
            .body("CPE1704TKS");
        }

        @GetMapping("fault")
        public void rspFault() {
            throw new RuntimeException("test");
        }

        @RequestMapping(path = "faultstatus-exception", method = {RequestMethod.GET, RequestMethod.PUT,
                RequestMethod.DELETE, RequestMethod.HEAD, RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.POST})
        public void rspStatusException() {
            throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT, "unauthorized");
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
