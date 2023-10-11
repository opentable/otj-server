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

import javax.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;

/**
 * A sample customizer that matches a regex to the path
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
    public boolean shouldFilter(HttpServletRequest httpServletRequest) {
        // Use the getRequestURI which is the path part of the uri (doesn't include protocol, host, or port)
        return pattern.matcher(httpServletRequest.getRequestURI()).matches();
    }
}
