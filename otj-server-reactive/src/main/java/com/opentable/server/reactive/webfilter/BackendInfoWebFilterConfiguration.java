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

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import com.opentable.server.BackendInfoFilterBaseConfiguration;
import com.opentable.service.AppInfo;
import com.opentable.service.ServiceInfo;

/**
 * Adds headers with prefix {@link #HEADER_PREFIX} with some information about the backend that actually
 * handled the request.
 */
@Configuration
@Import(BackendInfoWebFilterConfiguration.BackendInfoWebFilter.class)
public class BackendInfoWebFilterConfiguration extends BackendInfoFilterBaseConfiguration {

    public static class BackendInfoWebFilter implements WebFilter {
        private final Map<String, String> headers;

        BackendInfoWebFilter(final AppInfo appInfo, final ServiceInfo serviceInfo) {
            headers = assembleInfo(appInfo, serviceInfo);
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            headers.forEach((h,v) -> exchange.getResponse().getHeaders().add(h, v));
            return chain.filter(exchange);
        }
    }
}
