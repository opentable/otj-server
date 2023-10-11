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
package com.opentable.server.mvc;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.servlet.DispatcherType;
import java.util.Collection;

/**
 * This enables Spring's special handler for x-forwarded headers. If they are detected than many standard servlet
 * calls like getServerName return the x-forwarded-* value if present. Specifically:
 * getServerName() (x-forwarded-host)
 * getServerPort() (x-forwarded-for)
 * getScheme() (x-forwarded-proto)
 * isSecure() (derived from the getScheme)
 * sendRedirect(String) (relative to the new value)
 *
 * <p/>
 * While this is very convenient, particularly with Spring Security and OIDC, it can have unexpected behavior
 * in cases where libraries do not expect this and actually really did want the original host header value. After all
 * you really only need X-forwarded-Host if you want the ORIGINAL pre-ingress value.
 * <p/>
 * In these cases, you may (optionally) implement one or more beans implementing the ForwardedHeaderCustomizer, and
 * conditionally configure the filtration behavior. A good example is the RegexForwardedHeaderCustomizer.
 */
@Configuration
public class ForwardedFiltersConfiguration {


    @Bean
    FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter(
            Collection<ForwardedHeaderCustomizer> forwardedHeaderCustomizerList) {
        ForwardedHeaderFilter filter = new OverrideableForwardHeaderFilter(forwardedHeaderCustomizerList);
        FilterRegistrationBean<ForwardedHeaderFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
