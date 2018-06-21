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

    @Test
    public void test() {
        try (ConfigurableApplicationContext serverCtx = OTApplication.run(ServerConfiguration.class)) {
            final int serverPort = serverCtx.getBean(HttpServerInfo.class).getPort();
            final String serverTarget = String.format("http://localhost:%s/test", serverPort);
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
    @RestHttpServer
    @Import(TestMBeanServerConfiguration.class)
    static class ClientConfiguration {
        static final String SERVICE_NAME = "service-baz";
        @Bean
        ServiceInfo serviceInfo() {
            return new ServiceInfo() {
                @Override
                public String getName() {
                    return SERVICE_NAME;
                }
            };
        }
    }

    @Configuration
    @RestHttpServer
    @Import({
            TestResource.class,
            TestMBeanServerConfiguration.class,
    })
    static class ServerConfiguration {
        static final String SERVICE_NAME = "service-foo-bar";
        @Bean
        ServiceInfo serviceInfo() {
            return new ServiceInfo() {
                @Override
                public String getName() {
                    return SERVICE_NAME;
                }
            };
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
