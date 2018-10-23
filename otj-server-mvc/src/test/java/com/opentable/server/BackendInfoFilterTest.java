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

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
})
public class BackendInfoFilterTest extends AbstractTest {
    @Test
    public void test() {
        ResponseEntity<String> res = restTemplate.exchange("http://localhost:" + port + "/api/test", HttpMethod.GET, null, String.class);
        final AtomicBoolean sawBackendHeader = new AtomicBoolean();
        res.getHeaders().forEach((name, objs) -> {
            if (name.toLowerCase().startsWith(BackendInfoFilterConfiguration.HEADER_PREFIX.toLowerCase())) {
                Assert.assertTrue(!objs.isEmpty());
                final Object first = objs.get(0);
                Assert.assertTrue(first instanceof String);
                final String firstString = (String) first;
                Assert.assertFalse(firstString.isEmpty());
                sawBackendHeader.set(true);
            }
        });
        Assert.assertTrue(sawBackendHeader.get());
    }
}
