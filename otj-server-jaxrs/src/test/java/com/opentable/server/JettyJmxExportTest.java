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

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.common.collect.Iterables;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestJaxRsServerConfiguration.class
})
@DirtiesContext
public class JettyJmxExportTest {
    @Inject
    LoopbackRequest request;

    @Inject
    EmbeddedJetty ej;

    @Inject
    MBeanServer mbs;

    @Test(timeout = 10_000)
    public void testJettyExport() throws Exception {
        final ObjectName name = Iterables.getOnlyElement(mbs.queryNames(new ObjectName("org.eclipse.jetty.server:type=server,id=*"), null));
        assertThat(mbs.getAttribute(name, "state")).isEqualTo("STARTED");
    }

    @Test(timeout = 10_000)
    public void testJettyDumper() throws Exception {
        Thread.sleep(1000);
        assertThat(mbs.invoke(new ObjectName("com.opentable.server:name=com.opentable.server.JettyDumper,type=JettyDumper"), "dumpJetty", new Object[0], new String[0]).toString()).contains("default-pool");
    }
}
