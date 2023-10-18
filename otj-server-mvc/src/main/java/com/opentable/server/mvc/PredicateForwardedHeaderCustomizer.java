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
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * A sample customizer that matches a predicate
 */
public class PredicateForwardedHeaderCustomizer implements ForwardedHeaderCustomizer {
    public static ForwardedHeaderCustomizer regexURI(String pattern) {
        return regexURI(Pattern.compile(pattern));
    }

    public static ForwardedHeaderCustomizer regexURI(Pattern pattern) {
        return new PredicateForwardedHeaderCustomizer(httpServletRequest -> pattern.matcher(httpServletRequest.getRequestURI()).matches());
    }

    Predicate<HttpServletRequest> predicate;

    public PredicateForwardedHeaderCustomizer(Predicate<HttpServletRequest> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean shouldFilter(HttpServletRequest httpServletRequest) {
        return predicate.test(httpServletRequest);
    }
}
