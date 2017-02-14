package com.opentable.server;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.google.common.annotations.VisibleForTesting;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.service.AppInfo;
import com.opentable.service.ServiceInfo;

@Configuration
@Import(BackendInfoFilterConfiguration.BackendInfoFilter.class)
public class BackendInfoFilterConfiguration {
    @Bean
    public FilterRegistrationBean getBackendInfoFilterRegistrationBean(final BackendInfoFilter filter) {
        final FilterRegistrationBean reg = new FilterRegistrationBean(filter);
        reg.addUrlPatterns("/*");
        return reg;
    }

    /**
     * Adds header under {@link #HEADER_NAME} with some information about the backend that actually handled
     * the request.
     */
    public static class BackendInfoFilter implements Filter {
        @VisibleForTesting
        static final String HEADER_NAME = "X-OT-Backend";

        private final String info;

        BackendInfoFilter(final AppInfo appInfo, final ServiceInfo serviceInfo) {
            info = assembleInfo(appInfo, serviceInfo);
        }

        @Override
        public void init(final FilterConfig filterConfig) throws ServletException {}

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
                throws IOException, ServletException {
            try {
                chain.doFilter(request, response);
            } finally {
                if (response instanceof HttpServletResponse) {
                    final HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.addHeader(HEADER_NAME, info);
                }
            }
        }

        @Override
        public void destroy() {}

        /**
         * @return assemblage of service name, build tag, instance number, and task host, omitting any
         * individually unavailable components
         */
        private String assembleInfo(final AppInfo appInfo, final ServiceInfo serviceInfo) {
            final Integer instanceNo = appInfo.getInstanceNumber();
            final String instance = instanceNo != null ? String.format("instance-%s", instanceNo) : null;
            return Stream.of(
                    serviceInfo.getName(),
                    appInfo.getBuildTag(),
                    instance,
                    appInfo.getTaskHost()
            )
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("/"));
        }
    }
}
