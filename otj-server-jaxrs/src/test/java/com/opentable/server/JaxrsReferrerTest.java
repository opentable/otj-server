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

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.jaxrs.JaxRsClientFactory;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.service.ServiceInfo;

public class JaxrsReferrerTest {
    private static final String TASK_HOST = "mesos-agent-awesome.otenv.com";

    // This appears to be more a JAXRS Client test than a server test, since it primarily tests
    // whether certain headers like referring service and TASK_HOST are passed. These are not conserved.
    @Test
    public void test() {
        // http server....
        try (ConfigurableApplicationContext serverCtx = OTApplication.run(ServerConfiguration.class)) {
            final int serverPort = serverCtx.getBean(HttpServerInfo.class).getPort();
            final String serverTarget = String.format("http://localhost:%s/test", serverPort);
            // client
            try (ConfigurableApplicationContext clientCtx = OTApplication.run(
                    ClientConfiguration.class,
                    new String[]{},
                    ImmutableMap.of("TASK_HOST", TASK_HOST))
            ) {
                final JaxRsClientFactory clientFactory = clientCtx.getBean(JaxRsClientFactory.class);
                final Client internalClient = clientFactory
                        .newClient("test-client", StandardFeatureGroup.PLATFORM_INTERNAL);
                final Client externalClient = clientFactory
                        .newClient("test-client", StandardFeatureGroup.PUBLIC);
                try(final Response internalResp = internalClient.target(serverTarget).path("private").request().get()){
                    Assertions.assertThat(internalResp.getStatus()).isEqualTo(200);
                }

                try(final Response externalResp = externalClient.target(serverTarget).path("public").request().get()){
                    Assertions.assertThat(externalResp.getStatus()).isEqualTo(200);
                }

            }
        }
    }

    @Configuration
    @JAXRSServer
    @Import(TestMBeanServerConfiguration.class)
    static class ClientConfiguration {
        static final String SERVICE_NAME = "service-baz";
        @Bean
        ServiceInfo serviceInfo() {
            return () -> SERVICE_NAME;
        }
    }

    @Configuration
    @JAXRSServer
    @Import({
            TestResource.class,
            TestMBeanServerConfiguration.class,
    })
    static class ServerConfiguration {
        static final String SERVICE_NAME = "service-foo-bar";
        @Bean
        ServiceInfo serviceInfo() {
            return () -> SERVICE_NAME;
        }
    }

    @Path("/test")
    public static class TestResource {
        @GET
        @Path("private")
        public String expectSet(
                @HeaderParam("OT-ReferringService") final String referringService,
                @HeaderParam("OT-ReferringHost") final String referringHost) {
            Assertions.assertThat(referringService).isEqualTo(ClientConfiguration.SERVICE_NAME);
            Assertions.assertThat(referringHost).isEqualTo(TASK_HOST);
            return "ok";
        }

        @GET
        @Path("public")
        public String expectUnset(
                @HeaderParam("OT-ReferringService") final String referringService,
                @HeaderParam("OT-ReferringHost") final String referringHost) {
            Assertions.assertThat(referringService).isNull();
            Assertions.assertThat(referringHost).isNull();
            return "ok";
        }
    }
}
