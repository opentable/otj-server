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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.opentable.server.reactive.webfilter.BackendInfoWebFilterConfiguration;

@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
})
public class BackendInfoWebFilterTest extends AbstractTest {

    private static final String HEADER_PREFIX_LOWER_CASE = BackendInfoWebFilterConfiguration.HEADER_PREFIX.toLowerCase();

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void test() {

        FluxExchangeResult<String> result = webTestClient.get().uri("/api/test")
                .exchange()
                .returnResult(String.class);

        final AtomicBoolean sawBackendHeader = new AtomicBoolean();
        result.getResponseHeaders().forEach((headerName, headerValues) -> {
            if (headerName.toLowerCase().startsWith(HEADER_PREFIX_LOWER_CASE)) {
                Assert.assertTrue(!headerValues.isEmpty());
                final Object first = headerValues.get(0);
                Assert.assertTrue(first instanceof String);
                final String firstString = (String) first;
                Assert.assertFalse(firstString.isEmpty());
                sawBackendHeader.set(true);
            }
        });

        Assert.assertTrue(sawBackendHeader.get());
        assertEquals("some-service-3.14", result.getResponseHeaders().get(BackendInfoWebFilterConfiguration.HEADER_PREFIX + "Build-Tag").get(0));
        assertEquals("test", result.getResponseHeaders().get(BackendInfoWebFilterConfiguration.HEADER_PREFIX + "Service-Name").get(0));
        assertEquals("3", result.getResponseHeaders().get(BackendInfoWebFilterConfiguration.HEADER_PREFIX + "Instance-No").get(0));
        assertEquals("mesos-slave9001-dev-sf.qasql.opentable.com", result.getResponseHeaders().get(BackendInfoWebFilterConfiguration.HEADER_PREFIX + "Task-Host").get(0));
    }

}
