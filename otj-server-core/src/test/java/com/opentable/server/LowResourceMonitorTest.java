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

import org.eclipse.jetty.server.LowResourceMonitor;
import org.eclipse.jetty.server.Server;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
        TestServerConfiguration.class
})
@TestPropertySource(properties = {
        "ot.server.low-resource-monitor.enabled=true",
        "ot.server.low-resource-monitor.low-resource-idle-timeout-ms=1",
        "ot.server.low-resource-monitor.period-ms=2",
        "ot.server.low-resource-monitor.monitor-threads=false",
        "ot.server.low-resource-monitor.max-low-resources-time-ms=3",
        "ot.server.low-resource-monitor.accepting-in-low-resources=true"
})
// Basic test verify setup of Jetty's LowResourceMonitor
public class LowResourceMonitorTest {

    @Inject
    private Server server;

    @Test
    public void test() {
        LowResourceMonitor monitor = server.getBean(LowResourceMonitor.class);
        Assert.assertNotNull(monitor);
        Assert.assertEquals(1, monitor.getLowResourcesIdleTimeout());
        Assert.assertEquals(2, monitor.getPeriod());
        Assert.assertFalse(monitor.getMonitorThreads());
        Assert.assertEquals(3, monitor.getMaxLowResourcesTime());
        Assert.assertTrue(monitor.isAcceptingInLowResources());
    }

}
