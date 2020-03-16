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
package com.opentable.jaxrs.referrer;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.jackson.OpenTableJacksonConfiguration;
import com.opentable.jaxrs.JaxRsClientConfiguration;

/**
 * Sometimes, folks test with more minimal JAX-RS setups than a full service.  This test ensures that the referrer
 * filter functionality doesn't blow up in these circumstances.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
        OpenTableJacksonConfiguration.class,
        JaxRsClientConfiguration.class,
})
public class JaxrsReferrerMinimalTest {
    @Inject
    private ClientReferrerFilter filter;

    @Test
    @Ignore //TODO: broken for some reason
    public void test() {
        Assertions.assertThat(filter.isActive()).isFalse();
    }
}
