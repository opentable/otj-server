package com.opentable.server;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.security.RolesAllowed;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;

import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.webapp.WebAppContext;
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
    Consumer<WebAppContext> webAppContextCustomizer() {
        return webAppContext ->
                webAppContext.setErrorHandler(new TestErrorHandler());
    }

    @Bean
    ServletInitParameters servletInitParams() {
        return () -> ImmutableMap.of("resteasy.role.based.security", "true");
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

        @GET()
        @Path("5xx")
        public Response get5xx() {
            throw new RuntimeException("it has happened");
        }

        @GET
        public String get() {
            return TestServer.HELLO_WORLD;
        }

        @GET
        @Path("/nuclear-launch-codes")
        @RolesAllowed("POTUS")
        public String getLaunchCode() {
            return "CPE1704TKS";
        }

        @GET
        @Path("/async_join")
        // 'synchronized' will really induce indigestion if we aren't truly async
        public synchronized void asyncJoin(@QueryParam("n") int nJoiners, @QueryParam("i") int index, @Suspended AsyncResponse response) {
            waiters.add(response);
            int size = waiters.size();
            if (size == nJoiners) {
                LOG.info("Release the hounds!");
                waiters.forEach(r -> r.resume(ASYNC_JOIN_RESULT));
                LOG.info("Hounds dispatched.");
            } else {
                LOG.info("Parking #{} ({}) of {}", index, size, nJoiners);
            }
        }
    }

    public static class TestErrorHandler extends ErrorHandler {
        static final String TEXT = "you've been handled!";

        @Override
        protected void handleErrorPage(HttpServletRequest request, Writer writer, int code, String message)
                throws IOException {
            writer.write(TEXT);
            writer.flush();
        }
    }
}
