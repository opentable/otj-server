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

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.codahale.metrics.Counting;
import com.codahale.metrics.MetricRegistry;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    AuthorizationTest.TestConfiguration.class
})
@TestPropertySource(properties= {
    "ot.httpserver.max-threads=13",
    "ot.httpserver.static-path=static-test"
})
// This class primarily tests mvc's role support
// Unlike resteasy without spring security we can only test the servletrequest, not the actual @Role annotation
public class AuthorizationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Inject
    private MetricRegistry metrics;

    // 403 is returned
    @Test(timeout = 10_000)
    public void testAccessDenied_noRole() throws InterruptedException {
            ResponseEntity<?> r = testRestTemplate.getForEntity("/api/nuclear-launch-codes", Void.class);
            assertEquals(403, r.getStatusCodeValue());
            assertEquals("user",r.getHeaders().getFirst("X-Role-Inferred"));
    }

    @Test(timeout = 10_000)
    public void testAccessDenied_wrongRole() throws InterruptedException {
        ResponseEntity<?> r = testRestTemplate.getForEntity("/api/nuclear-launch-codes?role=PUTIN", Void.class);
        assertEquals(403, r.getStatusCodeValue());
        assertEquals("PUTIN",r.getHeaders().getFirst("X-Role-Inferred"));
    }

    @Test(timeout = 10_000)
    public void testAccessApproved() throws InterruptedException, IOException {
        ResponseEntity<?> r = testRestTemplate.getForEntity("/api/nuclear-launch-codes?role=POTUS", String.class);
        assertEquals(200, r.getStatusCodeValue());
        assertEquals("CPE1704TKS", r.getBody());
        assertEquals("POTUS",r.getHeaders().getFirst("X-Role-Inferred"));
        waitForCount("http-server.200-responses", 1);
    }

    // Not very robust since multiple members can pollute
    private void waitForCount(final String metricName, final long expected) throws InterruptedException {
        while (true) {
            final Counting c = (Counting) metrics.getMetrics().get(metricName);
            if (c != null && c.getCount() == expected) {
                break;
            }
            Thread.sleep(Duration.ofMillis(100).toMillis());
        }
    }

    @Configuration
    @Import(TestMvcServerConfiguration.class)
    public static class TestConfiguration {

        @Bean
        public FilterRegistrationBean<AuthorizationFilter> filterAFilterRegistrationBean() {
            return new FilterRegistrationBean<>(new AuthorizationFilter());
        }

    }


    public static class UserRoleRequestWrapper extends HttpServletRequestWrapper {

        private final String role;

        public UserRoleRequestWrapper(String role, HttpServletRequest request) {
            super(request);
            this.role = role;
        }

        @Override
        public boolean isUserInRole(String role) {
            return this.role.equals(role);
        }

        @Override
        public Principal getUserPrincipal() {
            // Sort of an abuse
            return new Principal() {
                @Override
                public String getName() {
                    return role;
                }
            };
        }
    }

    public static class AuthorizationFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        // Lets us inject a role as a query param - dubious in real life, but useful for tests
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String role = ObjectUtils.firstNonNull(request.getParameter("role"), "user");
            chain.doFilter(new UserRoleRequestWrapper(role, httpRequest), response);
        }

        @Override
        public void destroy() {
        }

    }

}
