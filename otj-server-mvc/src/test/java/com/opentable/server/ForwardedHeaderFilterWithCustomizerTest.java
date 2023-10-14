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

import com.opentable.server.mvc.EnableForwardedFilter;
import com.opentable.server.mvc.ForwardedHeaderCustomizer;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;


@Configuration
@Import(ForwardedHeaderFilterWithCustomizerTest.AddsCustomizer.class)
@EnableForwardedFilter
public class ForwardedHeaderFilterWithCustomizerTest extends AbstractTest {

    @Configuration
    public static class AddsCustomizer {
        @Bean
        public ForwardedHeaderCustomizer forwardedHeaderCustomizer() {
            return new PredicateForwardedHeaderCustomizer(httpServletRequest ->
                    httpServletRequest.getParameter("mike") != null);
        }
    }
    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void test() {
        // This line is needed because the default for testresttemplate is to use jdk url connection
        // which "protects" certain headers
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.HOST, "foofoo");
        httpHeaders.set("X-Forwarded-Host", "googoo");
        httpHeaders.set("X-Forwarded-Proto", "https");
        HttpEntity<String> httpEntity = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> response = testRestTemplate
                .exchange("/api/host?mike=there", HttpMethod.GET, httpEntity, String.class);
        assertEquals("https://googoo", response.getBody());

        response = testRestTemplate
                .exchange("/api/host", HttpMethod.GET, httpEntity, String.class);
        assertEquals("http://foofoo", response.getBody());;
    }

}
