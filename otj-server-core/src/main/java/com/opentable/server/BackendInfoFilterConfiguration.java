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
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

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
public class BackendInfoFilterConfiguration extends BackendInfoFilterBaseConfiguration {

    @Bean
    public FilterRegistrationBean<BackendInfoFilter> getBackendInfoFilterRegistrationBean(final BackendInfoFilter filter) {
        return new FilterRegistrationBean<>(filter);
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
    }
}
