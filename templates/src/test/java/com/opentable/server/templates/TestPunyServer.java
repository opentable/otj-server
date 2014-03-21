/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
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
package com.opentable.server.templates;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.name.Named;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kitei.testing.lessio.AllowNetworkAccess;
import org.kitei.testing.lessio.AllowNetworkListen;

import com.opentable.config.Config;
import com.opentable.config.ConfigModule;
import com.opentable.httpclient.HttpClient;
import com.opentable.httpclient.guice.HttpClientModule;
import com.opentable.httpclient.response.JsonContentConverter;
import com.opentable.lifecycle.junit.LifecycleRule;
import com.opentable.lifecycle.junit.LifecycleRunner;
import com.opentable.lifecycle.junit.LifecycleStatement;

@AllowNetworkAccess(endpoints={"127.0.0.1:*"})
@AllowNetworkListen(ports={0})
@RunWith(LifecycleRunner.class)
public class TestPunyServer
{
    @LifecycleRule
    public final LifecycleStatement lifecycleRule = LifecycleStatement.serviceDiscoveryLifecycle();

    @Inject
    @Named("test")
    public HttpClient httpClient;

    @Before
    public void setUp()
    {
        final Injector inj = Guice.createInjector(Stage.PRODUCTION,
                                                  new Module() {
                                                      @Override
                                                      public void configure(final Binder binder) {
                                                          binder.disableCircularProxies();
                                                          binder.requireExplicitBindings();
                                                      }
                                                  },
                                                  ConfigModule.forTesting(),
                                                  new HttpClientModule("test"),
                                                  lifecycleRule.getLifecycleModule());

        inj.injectMembers(this);

    }


    @Test
    public void testPunyServer() throws Exception
    {
        final PunyServer punyServer = new PunyServer() {
            @Override
            public Config getConfig()
            {
                return Config.getFixedConfig("ot.http.bind-port", "0");
            }
        };

        punyServer.startServer();
        Assert.assertTrue(punyServer.isStarted());

        final String baseUrl = String.format("http://localhost:%d/puny", punyServer.getPort());

        final Map<String, Object> result = httpClient.get(baseUrl, JsonContentConverter.getResponseHandler(new TypeReference<Map<String, Object>>() {}, new ObjectMapper())).perform();

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        final Object data = result.get("puny");
        Assert.assertEquals("result", data);

        punyServer.stopServer();
        Assert.assertTrue(punyServer.isStopped());
    }
}
