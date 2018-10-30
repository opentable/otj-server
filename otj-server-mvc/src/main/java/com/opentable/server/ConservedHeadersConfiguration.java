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

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.conservedheaders.ConservedHeaders;
import com.opentable.conservedheaders.ConservedHeadersFilter;
import com.opentable.scopes.threaddelegate.ThreadDelegatedScopeConfiguration;

/**
 * Add conserved headers to a Spring web application.
 *
 * Depends on the {@link com.opentable.scopes.threaddelegate.servlet.ThreadDelegatingScopeFilter}
 * being configured to run <em>before</em> the {@link ConservedHeadersFilter}.
 */
@Configuration
@Import({
        ThreadDelegatedScopeConfiguration.class,
        ConservedHeaders.class,
        ConservedHeadersFilter.class,
})
public class ConservedHeadersConfiguration {
    private static final String PATTERN = "/*";

    @Bean
    public FilterRegistrationBean<ConservedHeadersFilter> getConserveHeadersFilter(final ConservedHeadersFilter filter) {
        final FilterRegistrationBean<ConservedHeadersFilter> bean = new FilterRegistrationBean<>(filter);
        bean.addUrlPatterns(PATTERN);
        return bean;
    }

}
