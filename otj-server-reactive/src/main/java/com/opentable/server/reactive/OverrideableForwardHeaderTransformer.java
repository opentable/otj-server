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
package com.opentable.server.reactive;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

import java.util.Collection;

/**
 * Extension of the normal ForwardedHeaderTransformer that allows customizers to determine if the filter applies
 */
public class OverrideableForwardHeaderTransformer extends ForwardedHeaderTransformer {
    private final Collection<ForwardedHeaderCustomizer> forwardedHeaderCustomizers;

    public OverrideableForwardHeaderTransformer(Collection<ForwardedHeaderCustomizer> forwardedHeaderCustomizers) {
        this.forwardedHeaderCustomizers = forwardedHeaderCustomizers;
    }

    // Just to make things exciting
    // In servlet filter we returned true = skip, false = process
    // In reactive we return true to process, false to skip.
    protected boolean hasForwardedHeaders(ServerHttpRequest serverHttpRequest) {
        if (super.hasForwardedHeaders(serverHttpRequest)) {
            // if any say "do not filter", we will not filter, nor matter how many say "yes, filter"
            // for the case of no forwarded header customizers being added, this will return false, which
            // will process the filter unconditionally.
            return forwardedHeaderCustomizers.stream().allMatch(t -> t.shouldFilter(serverHttpRequest));
        }
        return false;
    }
}
