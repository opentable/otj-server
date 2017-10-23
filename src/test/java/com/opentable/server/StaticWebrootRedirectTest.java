package com.opentable.server;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.opentable.jaxrs.JaxRsClientFactory;
import com.opentable.jaxrs.JaxRsClientProperties;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.service.ServiceInfo;

public class StaticWebrootRedirectTest {
    @Inject
    private LoopbackRequest request;

    @Test
    public void testDefault() {
        try (ConfigurableApplicationContext ctx = runServer()) {
            test("static");
        }
    }

    @Test
    public void testCustom() {
        final String customPath = "fgsfds";
        try (ConfigurableApplicationContext ctx = runServer(customPath)) {
            test(customPath);
        }
    }

    private void test(final String path) {
        final Response r = request
                .of("")
                .property(JaxRsClientProperties.FOLLOW_REDIRECTS, false)
                .request()
                .get();
        Assertions.assertThat(r.getStatus()).isEqualTo(307);
        Assertions.assertThat(r.getLocation().getPath()).isEqualTo(String.format("/%s/index.html", path));
    }

    private ConfigurableApplicationContext runServer() {
        return runServer(null);
    }

    private ConfigurableApplicationContext runServer(final String staticPath) {
        final Map<String, Object> props;
        if (staticPath == null) {
            props = Collections.emptyMap();
        } else {
            props = Collections.singletonMap("ot.httpserver.static-path", staticPath);
        }
        final ConfigurableApplicationContext ctx = OTApplication.run(TestServer.class, new String[]{}, props);
        ctx.getAutowireCapableBeanFactory().autowireBean(this);
        return ctx;
    }

    @RestHttpServer
    @Import({
            StaticWebrootRedirect.class,
            LoopbackRequest.class,
    })
    public static class TestServer {
        @Path("/asdf")
        public String asdf() {
            return "asdf";
        }

        @Bean
        ServiceInfo serviceInfo() {
            return new ServiceInfo() {
                @Override
                public String getName() {
                    return "testy-mcserver";
                }
            };
        }

        @Bean(destroyMethod = "close")
        @Named("test")
        Client testClient(JaxRsClientFactory factory) {
            return factory.newClient("test", StandardFeatureGroup.PLATFORM_INTERNAL);
        }
    }
}
