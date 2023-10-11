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

import org.springframework.web.filter.ForwardedHeaderFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * Extension of the normal Spring Filter that allows customizers to decide if the filter applies
 */
public class OverrideableForwardHeaderFilter extends ForwardedHeaderFilter {
    private final Collection<ForwardedHeaderCustomizer> forwardedHeaderCustomizers;

    public OverrideableForwardHeaderFilter(Collection<ForwardedHeaderCustomizer> forwardedHeaderCustomizers) {
        this.forwardedHeaderCustomizers = forwardedHeaderCustomizers;
    }

    // returning true means skip, false means process the filter and hence potentially use x-forwarded headers
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!super.shouldNotFilter(request)) {
            // if any say "do not filter", we will not filter, nor matter how many say "yes, filter"
            // for the case of no forwarded header customizers being added, this will return false, which
            // will process the filter unconditionally.
            return forwardedHeaderCustomizers.stream().anyMatch(t -> !t.shouldFilter(request));
        }
        return true;
    }
}
