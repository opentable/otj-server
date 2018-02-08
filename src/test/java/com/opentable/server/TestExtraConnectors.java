package com.opentable.server;

import static com.opentable.server.EmbeddedJettyBase.DEFAULT_CONNECTOR_NAME;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.security.KeyStore;
import java.util.Map;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.opentable.jaxrs.JaxRsClientFactory;
import com.opentable.server.HttpServerInfo.ConnectorInfo;
import com.opentable.service.ServiceInfo;

public class TestExtraConnectors {

    private static final String SECURE_CONNECTOR_NAME = "secure-http";
    private static final String HTTP = "http", HTTPS = "https";
    private static final String PASSWORD = "verysecure";

    final URL keystore = Resources.getResource("test-ssl/keystore.jks");
    private Client client;

    @Before
    public void createClient() throws Exception {
        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(keystore.openStream(), PASSWORD.toCharArray());
        client = JaxRsClientFactory.testBuilder()
                .trustStore(trustStore)
                .build();
    }

    @After
    public void closeClient() {
        client.close();
    }

    @Test
    public void testExtraHttp() {
        withConnectors(Joiner.on(',').join(DEFAULT_CONNECTOR_NAME, SECURE_CONNECTOR_NAME), info -> {
            assertThat(info.getConnectors())
                .containsOnlyKeys(DEFAULT_CONNECTOR_NAME, SECURE_CONNECTOR_NAME);

            final ConnectorInfo defaultConnector = info.getConnectors().get(DEFAULT_CONNECTOR_NAME);

            assertThat(defaultConnector.getPort())
                .isGreaterThan(0);
            assertThat(defaultConnector.getProtocol())
                .isEqualTo("http");

            assertThat(client
                    .target(connector(HTTP, info, DEFAULT_CONNECTOR_NAME))
                    .request()
                    .get(String.class))
                .isEqualTo("boop");

            assertThat(client
                    .target(connector(HTTP, info, SECURE_CONNECTOR_NAME))
                    .request()
                    .get(String.class))
                .isEqualTo("boops");
        });
    }

    @Test
    public void testSsl() {
        withConnectors(HTTPS, info -> {
            assertThat(info.getConnectors())
                .containsOnlyKeys(HTTPS);

            assertThat(client
                    .target(connector(HTTPS, info, HTTPS))
                    .request()
                    .get(String.class))
                .isEqualTo("boops");
        });
    }

    private String connector(String protocol, HttpServerInfo info, String name) {
        return String.format("%s://localhost:%s/beep", protocol, info.getConnectors().get(name).getPort());
    }

    private void withConnectors(String connectorList, Consumer<HttpServerInfo> action) {
        final Map<String, Object> props = ImmutableMap.<String, Object>builder()
                .put("PORT0", 32743)
                .put("PORT1", 32744)
                .put("ot.httpserver.connector.secure-http.forceSecure", true)
                .put("ot.httpserver.connector.https.protocol", "https")
                .put("ot.httpserver.connector.https.keystore", keystore)
                .put("ot.httpserver.connector.https.keystorePassword", PASSWORD)
                .put("ot.httpserver.active-connectors", connectorList)
                .put("ot.jmx.port", 0)
            .build();
        try (ConfigurableApplicationContext ctx = OTApplication.run(TestingServer.class, new String[0], props)) {
            action.accept(ctx.getBean(HttpServerInfo.class));
        }
    }

    @RestHttpServer
    @Import(TestingServer.TestingResource.class)
    public static class TestingServer {
        @Bean
        ServiceInfo info() {
            return () -> "connector-test";
        }

        @Path("/beep")
        public static class TestingResource {
            @GET
            public String beep(@Context SecurityContext secure) {
                return "boop" + (secure.isSecure() ? "s" : "");
            }
        }
    }
}

