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

import javax.inject.Inject;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestJaxRsServerConfiguration.class,
    EmbeddedReactiveJetty.class
})
@TestPropertySource(properties = {
        "ot.httpserver.max-threads=" + AsyncBaseTest.N_THREADS,
        "jaxrs.client.default.connectionPoolSize=128"
})
public class AsyncReactiveTest extends AsyncBaseTest {

    @Inject
    EmbeddedReactiveJetty ejr;

    @Override
    protected EmbeddedJettyBase getEmbeddedJetty() {
        return ejr;
    }
}
