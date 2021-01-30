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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.components.filterorder.OrderDeclaration;

public class FilterOrderTest {
    private static final String BOOL_NAME = "filter-callback-bool";

    @Inject
    private HttpServerInfo serverInfo;

    @Inject
    @Named("test")
    private Client testClient;

    // Test the ordering imposed by otj-filterorder
    @Test
    public void testAB() {
        test(TestConfigurationAB.class);
    }

    @Test
    public void testBA() {
        test(TestConfigurationBA.class);
    }

    @Test(expected = ApplicationContextException.class)
    public void testSuperfluousLast() {
        try (ConfigurableApplicationContext ctx = OTApplication.run(AnotherLast.class)) {
        }
    }

    @Test(expected = ApplicationContextException.class)
    public void testCycle() {
        test(Cycle.class);
    }

    private void test(final Class<? extends TestConfigurationBase> configurationClass) {
        try (ConfigurableApplicationContext ctx = OTApplication.run(configurationClass)) {
            ctx.getBeanFactory().autowireBean(this);
            final AtomicBoolean testsPassed = ctx.getBean(BOOL_NAME, AtomicBoolean.class);
            final String target = String.format("http://localhost:%s/", serverInfo.getPort());
            try (Response resp = testClient
                    .target(target)
                    .request()
                    .get()) {
                Assertions.assertThat(resp.getStatus()).isEqualTo(200);
                Assertions.assertThat(testsPassed.get()).isTrue();
            }
        }
    }

    @Configuration
    @Import(TestJaxRsServerConfiguration.class)
    public static class TestConfigurationBase {
        @Bean
        public FilterRegistrationBean<FilterA> filterAFilterRegistrationBean(final Callback cb) {
            return new FilterRegistrationBean<>(new FilterA(cb));
        }

        @Bean
        public FilterRegistrationBean<FilterB> filterBFilterRegistrationBean(final Callback cb) {
            return new FilterRegistrationBean<>(new FilterB(cb));
        }

        @Bean(name = BOOL_NAME)
        public AtomicBoolean testsPassed() {
            return new AtomicBoolean();
        }
    }

    @Configuration
    public static class TestConfigurationAB extends TestConfigurationBase {
        @Bean
        public OrderDeclaration testOrderDeclaration() {
            return OrderDeclaration
                    .of(FilterB.class)
                    .dependsOn(FilterA.class);
        }

        @Bean
        public Callback filterCallback(
                @Named(BOOL_NAME) final AtomicBoolean testsPassed) {
            final AtomicBoolean a = new AtomicBoolean();
            return cls -> {
                if (cls == FilterA.class) {
                    a.set(true);
                } else if (cls == FilterB.class) {
                    if (a.get()) {
                        testsPassed.set(true);
                    }
                }
            };
        }
    }

    @Configuration
    public static class TestConfigurationBA extends TestConfigurationBase {
        @Bean
        public OrderDeclaration testOrderDeclaration() {
            return OrderDeclaration
                    .of(FilterA.class)
                    .dependsOn(FilterB.class);
        }

        @Bean
        public Callback filterCallback(
                @Named(BOOL_NAME) final AtomicBoolean testsPassed) {
            final AtomicBoolean b = new AtomicBoolean();
            return cls -> {
                if (cls == FilterA.class) {
                    if (b.get()) {
                        testsPassed.set(true);
                    }
                } else if (cls == FilterB.class) {
                    b.set(true);
                }
            };
        }
    }

    @Configuration
    @Import(TestJaxRsServerConfiguration.class)
    public static class AnotherLast {
        @Bean
        public OrderDeclaration anotherLast() {
            return OrderDeclaration.last(FilterA.class);
        }
    }

    @Configuration
    public static class Cycle extends TestConfigurationAB {
        @Bean
        public OrderDeclaration baOrderDeclaration() {
            return OrderDeclaration
                    .of(FilterA.class)
                    .dependsOn(FilterB.class);
        }
    }

    public static class CallbackFilter implements Filter {
        private final Callback cb;

        public CallbackFilter(final Callback cb) {
            this.cb = cb;
        }

        @Override
        public void init(FilterConfig filterConfig) {
        }

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            this.cb.call(this.getClass());
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {
        }
    }

    public static class FilterA extends CallbackFilter {
        public FilterA(final Callback cb) {
            super(cb);
        }
    }

    public static class FilterB extends CallbackFilter {
        public FilterB(final Callback cb) {
            super(cb);
        }
    }

    public interface Callback {
        void call(Class<? extends CallbackFilter> cls);
    }
}
