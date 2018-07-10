package com.opentable.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ManagementFactory;

import javax.inject.Inject;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class
})
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class JettyJmxExportTest {
    @Inject
    LoopbackRequest request;

    @Inject
    EmbeddedJetty ej;

    @Test(timeout = 10_000)
    public void testMBeans() throws Exception {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        assertThat(mbs.getAttribute(new ObjectName("org.eclipse.jetty.server:type=server,id=0"), "state")).isEqualTo("STARTED");
    }
}
