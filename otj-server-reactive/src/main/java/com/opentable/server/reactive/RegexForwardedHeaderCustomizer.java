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

import java.util.regex.Pattern;

/**
 * A sample customizer using a Regex against the path
 */
public class RegexForwardedHeaderCustomizer implements ForwardedHeaderCustomizer {
    private final Pattern pattern;

    public RegexForwardedHeaderCustomizer(Pattern pattern) {
        this.pattern = pattern;
    }

    public RegexForwardedHeaderCustomizer(String pattern) {
        this(Pattern.compile(pattern));
    }

    @Override
    public boolean shouldFilter(ServerHttpRequest serverHttpRequest) {
        // Use the path which is the path part of the uri (doesn't include protocol, host, or port)
        return pattern.matcher(serverHttpRequest.getPath().pathWithinApplication().value()).matches();
    }
}
