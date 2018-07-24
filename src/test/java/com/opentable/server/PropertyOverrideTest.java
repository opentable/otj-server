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
