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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class HealthCheckTest extends AbstractTest {

    @Autowired
    public TestRestTemplate testRestTemplate;

    @Test
    public void testHealthEndpoint() {
        ResponseEntity<String> healthResponse = testRestTemplate.getForEntity("/health", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testServiceStatusEndpoint() {
        ResponseEntity<String> healthResponse = testRestTemplate.getForEntity("/service-status", String.class);
        assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}

