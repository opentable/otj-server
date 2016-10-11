package com.opentable.server;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    public static final String ASYNC_JOIN_RESULT = "Welcome to the asynchronous future";

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
    @Singleton
    @Path("/")
    public static class TestResource {
        private static final Logger LOG = LoggerFactory.getLogger(TestResource.class);
        private final List<AsyncResponse> waiters = new ArrayList<>();

        @GET
        public String get() {
            return TestServer.HELLO_WORLD;
        }

        @GET
        @Path("/async_join")
        // 'synchronized' will really induce indigestion if we aren't truly async
        public synchronized void asyncJoin(@QueryParam("n") int nJoiners, @Suspended AsyncResponse response) {
            waiters.add(response);
            int size = waiters.size();
            if (size == nJoiners) {
                LOG.info("Release the hounds!");
                waiters.forEach(r -> r.resume(ASYNC_JOIN_RESULT));
            } else {
                LOG.info("Parking #{} of {}", size, nJoiners);
            }
        }
    }
}
