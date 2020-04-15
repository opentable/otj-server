package com.opentable.server;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles(profiles = "deployed")
@ContextConfiguration(classes = {
        TestServerConfiguration.class
})
@TestPropertySource(properties = {
        "OT_BUILD_TAG=some-service-3.14",
        "INSTANCE_NO=3",
        "PORT_ACTUATOR=9999",
//        "management.server.port=50",
        "TASK_HOST=mesos-slave9001-dev-sf.qasql.opentable.com",
})
public class SpringPortSelectionPostProcessorTest {

    @Inject
    private Environment environment;

    @Test
    // Shows PORT_ACTUATOR is picked up and takes, but has lowest priority
    public void testActuator() {
        Assert.assertEquals("9999", environment.getProperty(SpringPortSelectionPostProcessor.MANAGEMENT_SERVER_PORT));
    }
}