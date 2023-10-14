
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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.Assert.assertEquals;

@EnableForwardedFilter
@Configuration
@Import(ForwardedHeaderFilterWithCustomizerTest.AddsCustomizer.class)
public class ForwardedHeaderFilterWithCustomizerTest extends AbstractTest {

    @Configuration
    public static class AddsCustomizer {
        @Bean
        public ForwardedHeaderCustomizer forwardedHeaderCustomizer() {
            return new PredicateForwardedHeaderCustomizer(serverHttpRequest ->
                    serverHttpRequest.getQueryParams().get("mike") != null);
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void test() {
        FluxExchangeResult<String> result = webTestClient.get()
                .uri("/api/host?mike=foo")

                .header("X-Forwarded-Host", "googoo")
                .header("X-Forwarded-Proto", "https")
                .header("Host", "foofoo")
                .exchange()
                .returnResult(String.class);
        String uri = result.getResponseBody().blockFirst();
        assertEquals("https://googoo/api/host?mike=foo", uri);

        result = webTestClient.get()
                .uri("/api/host")

                .header("X-Forwarded-Host", "googoo")
                .header("X-Forwarded-Proto", "https")
                .header("Host", "foofoo")
                .exchange()
                .returnResult(String.class);
        uri = result.getResponseBody().blockFirst();
        assertEquals("http://foofoo/api/host", uri);
    }

}
