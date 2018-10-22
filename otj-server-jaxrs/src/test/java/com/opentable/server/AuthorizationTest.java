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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
import javax.ws.rs.core.Response;

import com.codahale.metrics.Counting;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
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
    "ot.httpserver.static-path=static-test",
})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthorizationTest {

    @Inject
    private LoopbackRequest request;

    @Inject
    private MetricRegistry metrics;

    @Test(timeout = 10_000)
    public void testAccessDenied_noRole() throws InterruptedException {
        try(Response r = request.of("/nuclear-launch-codes").request().get()){
            assertEquals(403, r.getStatus());
            waitForCount("http-server.403-responses", 1);
        }
    }

    @Test(timeout = 10_000)
    public void testAccessDenied_wrongRole() throws InterruptedException {
        try(Response r = request.of("/nuclear-launch-codes").queryParam("role", "PUTIN").request().get()){
            assertEquals(403, r.getStatus());
            waitForCount("http-server.403-responses", 1);
        }
    }

    @Test(timeout = 10_000)
    public void testAccessApproved() throws InterruptedException, IOException {
        try(Response r = request.of("/nuclear-launch-codes").queryParam("role", "POTUS").request().get()){
            assertEquals(200, r.getStatus());
            assertEquals("CPE1704TKS", IOUtils.toString(((ByteArrayInputStream) r.getEntity()), StandardCharsets.UTF_8));
            waitForCount("http-server.200-responses", 1);
        }
    }

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
    @Import(TestJaxRsServerConfiguration.class)
    public static class TestConfiguration {

        @Bean
        public FilterRegistrationBean<AuthorizationFilter> filterAFilterRegistrationBean() {
            return new FilterRegistrationBean<>(new AuthorizationFilter());
        }

        @Bean
        ServletInitParameters servletInitParams() {
            return () -> ImmutableMap.of("resteasy.role.based.security", "true");
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

    }

    public static class AuthorizationFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

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
