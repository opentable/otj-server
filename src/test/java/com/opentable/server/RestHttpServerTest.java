package com.opentable.server;

import java.io.IOException;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import com.opentable.service.ServiceInfo;

@RunWith(SpringRunner.class)
@Configuration
public class RestHttpServerTest {
    private ConfigurableApplicationContext context;
    private Integer port;

    @Before
    public void before() throws InterruptedException {
        Assert.assertNull(context);
        Assert.assertNull(port);
        context = SpringApplication.run(TestServer.class);
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
    }

    @Test
    public void testHello() throws IOException {
        final CloseableHttpClient client = HttpClients.createMinimal();
        final HttpGet get = new HttpGet("http://localhost:" + port);
        try (final CloseableHttpResponse resp = client.execute(get)) {
            Assert.assertEquals(200, resp.getStatusLine().getStatusCode());
            final HttpEntity ent = resp.getEntity();
            Assert.assertEquals(TestResource.HELLO_WORLD, IOUtils.toString(ent.getContent()));
        }
    }

    @Configuration
    @RestHttpServer
    @Import(TestResource.class)
    public static class TestServer {
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
    }

    @Named
    @Path("/")
    public static class TestResource {
        static final String HELLO_WORLD = "Hello, world!";
        @GET
        public String get() {
            return HELLO_WORLD;
        }
    }
}
