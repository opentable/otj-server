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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = {
    TestServer.class
})
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
        assertThat(mbs.invoke(new ObjectName("com.opentable.server:name=com.opentable.server.JettyDumper,type=JettyDumper"), "dumpJetty", new Object[0], new String[0]).toString()).contains("default-pool");
    }
}
