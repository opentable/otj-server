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
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
// Tests some very basic json marshalling. Probably could be expanded
public class MessageConverterTest extends AbstractTest {

    @Autowired
    private TestRestTemplate restTemplate;


    private TestMvcServerConfiguration.MrBean getMrBean(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForObject("/api/map",
            new HttpEntity<>(body, headers), TestMvcServerConfiguration.MrBean.class);
    }

    @Test
    public void testDeserialization() {
        TestMvcServerConfiguration.MrBean mrBean = restTemplate.getForObject("/api/rsp", TestMvcServerConfiguration.MrBean.class);
        assertEquals("1", mrBean.getBar());
        assertEquals("2", mrBean.getFoo());
    }

    @Test
    public void testParameterNames() {
        TestMvcServerConfiguration.MrBean mrBean = getMrBean("{\"bar\":\"1\",\"foo\":\"2\"}");
        assertEquals("1", mrBean.getBar());
        assertEquals("2", mrBean.getFoo());
    }

    @Test
    public void testBadParameterName() {
        TestMvcServerConfiguration.MrBean mrBean = getMrBean("{\"bax\":\"1\",\"foo\":\"2\"}");
        assertNull(mrBean.getBar());
        assertEquals("2", mrBean.getFoo());
    }

}
