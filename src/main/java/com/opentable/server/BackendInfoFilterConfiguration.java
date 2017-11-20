package com.opentable.server;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableMap;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.service.AppInfo;
import com.opentable.service.ServiceInfo;

/**
 * Adds headers with prefix {@link #HEADER_PREFIX} with some information about the backend that actually
 * handled the request. The Front Door filters out these headers for public-facing instances.
 */
@Configuration
@Import(BackendInfoFilterConfiguration.BackendInfoFilter.class)
public class BackendInfoFilterConfiguration {
    public static final String HEADER_PREFIX = "X-OT-Backend-";

    @Bean
    public FilterRegistrationBean getBackendInfoFilterRegistrationBean(final BackendInfoFilter filter) {
        return new FilterRegistrationBean(filter);
    }

    public static class BackendInfoFilter implements Filter {
        private final Map<String, String> headers;

        BackendInfoFilter(final AppInfo appInfo, final ServiceInfo serviceInfo) {
            headers = assembleInfo(appInfo, serviceInfo);
        }

        @Override
        public void init(final FilterConfig filterConfig) throws ServletException {}

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            // If transfer encoding ends up being chunked, setting these after execution of the filter chain results
            // in these added headers being ignored.  We therefore add them before chain execution.  See OTPL-1698.
            if (response instanceof HttpServletResponse) {
                final HttpServletResponse httpResponse = (HttpServletResponse) response;
                headers.forEach(httpResponse::addHeader);
            }
            chain.doFilter(request, response);
        }

        @Override
        public void destroy() {}

        /**
         * @return map of headers we'll add to responses; unavailable information will result in headers
         * not being set
         */
        private Map<String, String> assembleInfo(final AppInfo appInfo, final ServiceInfo serviceInfo) {
            final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
            builder.put(named("Service-Name"), serviceInfo.getName());

            if (appInfo.getBuildTag() != null) {
                builder.put(named("Build-Tag"), appInfo.getBuildTag());
            }

            final Integer instanceNo = appInfo.getInstanceNumber();
            if (instanceNo != null) {
                builder.put(named("Instance-No"), instanceNo.toString());
            }

            if (appInfo.getTaskHost() != null) {
                builder.put(named("Task-Host"), appInfo.getTaskHost());
            }

            return builder.build();
        }

        private static String named(final String name) {
            return HEADER_PREFIX + name;
        }
    }
}
