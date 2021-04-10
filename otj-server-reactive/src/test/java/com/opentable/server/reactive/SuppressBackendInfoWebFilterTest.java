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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.opentable.server.BackendInfoFilterConfiguration;
@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "ot.server.backend.info.enabled=false"
})
// Check if the OT-Backend stuff is NOT propagated
public class SuppressBackendInfoWebFilterTest extends AbstractTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void test() {
        FluxExchangeResult<Void> result = webTestClient.get().uri("/api/test")
                .exchange()
                .returnResult(Void.class);
        Assert.assertFalse(result.getResponseHeaders().keySet().stream()
                .anyMatch(name ->  name.toLowerCase()
                        .startsWith(BackendInfoFilterConfiguration.HEADER_PREFIX.toLowerCase())));
    }

}
