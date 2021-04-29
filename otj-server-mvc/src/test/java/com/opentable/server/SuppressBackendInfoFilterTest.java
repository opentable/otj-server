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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
        "ot.server.backend.info.enabled=false"
})
// Tests returning the OT-Backend stuff in a response
public class SuppressBackendInfoFilterTest  extends AbstractTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    //Call an endpoint and verify the OT-Backend prefixes are NOT returned
    @Test
    public void test() {
        ResponseEntity<String> response = testRestTemplate.exchange("/api/test", HttpMethod.GET, null, String.class);
            Assert.assertEquals(response.getStatusCodeValue(), 200);
            Assert.assertFalse(response.getHeaders().keySet().stream()
                    .anyMatch(name ->  name.toLowerCase()
                            .startsWith(BackendInfoFilterConfiguration.HEADER_PREFIX.toLowerCase())));
    }

}
