package com.opentable.server;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.jaxrs.JaxRsClientFactory;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.service.ServiceInfo;

@Configuration
@RestHttpServer
@Import({
    TestServer.TestResource.class,
    LoopbackRequest.class,
})
public class TestServer {
    public static final String HELLO_WORLD = "Hello, world!";

    public static void main(final String[] args) {
        SpringApplication.run(TestServer.class, args);
    }

    @Bean
    @Named("test")
    Client testClient(JaxRsClientFactory factory) {
        return factory.newClient("test", StandardFeatureGroup.PLATFORM_INTERNAL);
    }

    @Configuration
    public static class ServiceInfoConfiguration {
        @Bean
        public ServiceInfo getServiceInfo() {
            return new ServiceInfo() {
                @Override
                public String getName() {
                    return "test";
                }
            };
        }
    }

    @Named
    @Path("/")
    public static class TestResource {
        @GET
        public String get() {
            return TestServer.HELLO_WORLD;
        }
    }
}
