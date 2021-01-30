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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.net.ServerSocket;

import javax.ws.rs.client.Client;

import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import com.opentable.jaxrs.JaxRsClientFactory;

// Tests allocating multiple http ports
public class DoublePortTest {
    private ConfigurableApplicationContext context;
    private Integer port;
    private int secondPort;
    private Client client;

    @Before
    public void before() throws Exception {
        client = JaxRsClientFactory.testBuilder().build();
        try (ServerSocket ss = new ServerSocket(0)) {
            secondPort = ss.getLocalPort();
        }
        Assert.assertNull(context);
        Assert.assertNull(port);
        SpringApplication app = new SpringApplication(TestServerConfiguration.class);
        // Two http ports
        app.setDefaultProperties(ImmutableMap.of(
                "ot.httpserver.active-connectors", "default-http,extra-http",
                "ot.httpserver.connector.extra-http.port", secondPort));
        context = app.run();
        port = context.getBeanFactory().getBean(HttpServerInfo.class).getPort();
    }

    @After
    public void after() {
        Assert.assertNotNull(context);
        Assert.assertNotNull(port);
        port = null;
        context.stop();
        context.close();
        context = null;
        client.close();
    }

    // Show each port returns an expected response when HTTP GET is called
    @Test
    public void testPorts() throws Exception {
        assertNotSame(port, secondPort);
        assertEquals(TestServerConfiguration.HELLO_WORLD, readHello(port));
        assertEquals(TestServerConfiguration.HELLO_WORLD, readHello(secondPort));
    }

    private String readHello(int myPort) {
        return client.target("http://localhost:" + myPort + "/hello").request().get().readEntity(String.class);
    }
}
