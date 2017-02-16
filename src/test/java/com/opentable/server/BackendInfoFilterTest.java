package com.opentable.server;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

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
        TestServer.class
})
@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
})
public class BackendInfoFilterTest {
    @Inject
    private LoopbackRequest request;

    @Test
    public void test() {
        final Response r = request.of("/").request().get();
        final AtomicBoolean sawBackendHeader = new AtomicBoolean();
        r.getHeaders().forEach((name, objs) -> {
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
    }
}
