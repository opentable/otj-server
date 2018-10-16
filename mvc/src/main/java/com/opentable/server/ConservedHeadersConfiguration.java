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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.conservedheaders.ClientConservedHeadersFeature;
import com.opentable.jaxrs.JaxRsFeatureBinding;
import com.opentable.jaxrs.StandardFeatureGroup;

@Configuration
@Import(com.opentable.conservedheaders.ConservedHeadersConfiguration.class)
public class ConservedHeadersConfiguration {
    @Bean
    JaxRsFeatureBinding getConserveHeadersFeatureBinding(final ClientConservedHeadersFeature feature) {
        return JaxRsFeatureBinding.bind(StandardFeatureGroup.PLATFORM_INTERNAL, feature);
    }
}
