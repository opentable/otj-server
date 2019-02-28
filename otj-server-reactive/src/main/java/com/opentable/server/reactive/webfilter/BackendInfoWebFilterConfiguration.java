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
package com.opentable.server.reactive.webfilter;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import com.opentable.service.AppInfo;
import com.opentable.service.ServiceInfo;

/**
 * Adds headers with prefix {@link #HEADER_PREFIX} with some information about the backend that actually
 * handled the request.
 */
@Configuration
@Import(BackendInfoWebFilterConfiguration.BackendInfoFilter.class)
public class BackendInfoWebFilterConfiguration {

    private static final String HEADER_PREFIX = "X-OT-Backend-";

    public static class BackendInfoFilter implements WebFilter {
        private final Map<String, String> headers;

        BackendInfoFilter(final AppInfo appInfo, final ServiceInfo serviceInfo) {
            headers = assembleInfo(appInfo, serviceInfo);
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            headers.forEach((h,v) -> exchange.getResponse().getHeaders().add(h, v));
            return chain.filter(exchange);
        }

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
