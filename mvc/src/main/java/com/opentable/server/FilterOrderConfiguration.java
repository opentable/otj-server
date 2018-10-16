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

import org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.components.filterorder.FilterOrderResolverConfiguration;
import com.opentable.components.filterorder.OrderDeclaration;

@Configuration
@Import(FilterOrderResolverConfiguration.class)
public class FilterOrderConfiguration {
    /**
     * {@link org.jboss.resteasy.plugins.server.servlet.Filter30Dispatcher} must come last because it is the filter
     * that does the <em>actual</em> work of handling the request; it is thus terminal.
     * @return OrderDeclaration configured as the last item.
     */
    @Bean
    public OrderDeclaration filter30OrderDeclaration() {
        return OrderDeclaration.last(Filter30Dispatcher.class);
    }
}
