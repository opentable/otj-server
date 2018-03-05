package com.opentable.server;

import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

// See application.properties in test resources for properties subject to override in this test.

public class PropertyOverrideTest {
    ConfigurableApplicationContext ctx;

    @Value("${test.a}")
    String testA;

    @Value("${test.b}")
    String testB;

    @Before
    public void before() {
        Assert.assertNull(ctx);
        ctx = OTApplication.run(Object.class, new String[]{}, ImmutableMap.of("test.a", "3"),
                builder -> builder.web(WebApplicationType.NONE));
        ctx.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @After
    public void after() {
        Assert.assertNotNull(ctx);
        ctx.close();
        ctx = null;
    }

    @Test
    public void test() {
        Assert.assertEquals("3", testA);
        Assert.assertEquals("2", testB);
    }
}
