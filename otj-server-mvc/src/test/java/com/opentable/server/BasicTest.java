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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.opentable.logging.CommonLogHolder;

public class BasicTest extends AbstractTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    // Sets the serviceType
    @Test
    public void applicationLoads() {
        assertEquals("test", CommonLogHolder.getServiceType());
    }

    // Port as expected
    @Test
    public void httpServerInfoMatchesEnvironment() {
        assertEquals(port, httpServerInfo.getPort());
    }

    // Basic get call
    @Test
    public void testApiCall() {
        String res = testRestTemplate.getForObject("/api/test", String.class);
        assertEquals("test", res);
    }

}
