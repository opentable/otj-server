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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
        TestJaxRsServerConfiguration.class
})
@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "ot.server.backend.info.enabled=true"
})
// NB Core.BackendFilterTest appears redundant and should probably be whacked
public class JAXRSBackendInfoFilterTest {
    @Inject
    private JAXRSLoopbackRequest request;

    @Test
    public void test() {
        try(final Response response = request.of("/").request().get()){
            final AtomicBoolean sawBackendHeader = new AtomicBoolean();
            response.getHeaders().forEach((name, objs) -> {
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
            assertEquals("some-service-3.14", response.getHeaders().get(BackendInfoFilterConfiguration.HEADER_PREFIX + "Build-Tag").get(0));
            assertEquals("test", response.getHeaders().get(BackendInfoFilterConfiguration.HEADER_PREFIX + "Service-Name").get(0));
            assertEquals("3", response.getHeaders().get(BackendInfoFilterConfiguration.HEADER_PREFIX + "Instance-No").get(0));
            assertEquals("mesos-slave9001-dev-sf.qasql.opentable.com", response.getHeaders().get(BackendInfoFilterConfiguration.HEADER_PREFIX + "Task-Host").get(0));

        }
    }
}
