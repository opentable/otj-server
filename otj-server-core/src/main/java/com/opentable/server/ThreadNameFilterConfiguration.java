package com.opentable.server;

import java.io.IOException;
import java.time.Instant;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.stereotype.Component;


@Configuration
@Conditional(ThreadNameFilterConfiguration.InstallThreadNameFilter.class)
@Import(ThreadNameFilterConfiguration.ThreadNameFilter.class)
public class ThreadNameFilterConfiguration {

    public static class InstallThreadNameFilter implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            final String value = context.getEnvironment().
                getProperty("ot.server.thread-name-filter", "true");
            return "true".equals(value);
        }
    }

    @Bean
    public FilterRegistrationBean<ThreadNameFilter> getThreadNameFilterRegistrationBean(final ThreadNameFilter filter) {
        return new FilterRegistrationBean<>(filter);
    }

    @Component
    public static class ThreadNameFilter implements Filter {

        private static final Logger LOG = LoggerFactory.getLogger(ThreadNameFilterConfiguration.ThreadNameFilter.class);

        @Override
        public void init(FilterConfig filterConfig) {
            LOG.info("Thread name tracking enabled.");
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
            final HttpServletRequest req = (HttpServletRequest) request;
            final String name = Thread.currentThread().getName();
            try {
                try {
                    Thread.currentThread()
                        .setName(String.format("%s:%s", Instant.now().toString(), req.getRequestURI()));
                } finally {
                    chain.doFilter(request, response);
                }
            } finally {
                Thread.currentThread().setName(name);
            }
        }

        @Override
        public void destroy() {

        }
    }

}
