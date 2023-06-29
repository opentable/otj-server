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

import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opentable.server.mvc.MVCServer;
import com.opentable.service.ServiceInfo;

public class SNIHostCheckTests {

    private static final String SECURE_CONNECTOR_NAME = "secure-http";
    private static final String HTTP = "http", HTTPS = "https";
    private static final String PASSWORD = "verysecure";

    final URL keystore = Resources.getResource("test-ssl/keystore.jks");

    private HttpClient client;

    private SNIHostName sni = null;

    @Before
    public void createClient() throws Exception {
        final KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(keystore.openStream(), PASSWORD.toCharArray());

        final SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
        ClientConnector clientConnector = new ClientConnector();
        clientConnector.setSslContextFactory(sslContextFactory);

        sslContextFactory.setKeyStore(trustStore);
        sslContextFactory.setTrustStore(trustStore);
        sslContextFactory.setKeyManagerPassword(PASSWORD);

        sslContextFactory.setTrustAll(true);
        sslContextFactory.setSNIProvider(new SniProvider() {
            @Override
            public List<SNIServerName> apply(SSLEngine sslEngine, List<SNIServerName> serverNames) {
                if (sni != null) {
                    return Collections.singletonList(sni);
                }
                return null;
            }
        });
        client = new HttpClient(new HttpClientTransportDynamic(clientConnector));
        client.setFollowRedirects(false);
        client.setRequestBufferSize(16384);
        client.setResponseBufferSize(16384);
        client.start();
    }

    @Test
    public void SslWithValidSNI() {
        sni = new SNIHostName("test.com");
        withConnectors(HTTPS, false, info -> {
            assertThat(info.getConnectors())
                .containsOnlyKeys(HTTPS);
            try {
                assertThat(client.newRequest(connector(HTTPS, info, HTTPS))
                    .headers(headers -> headers.add(HttpHeader.HOST, "test.com"))
                    .method(HttpMethod.GET)
                    .send()
                    .getContentAsString())
                    .isEqualTo("boop");
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void SslWithInValidSNIShouldFail() {
        sni = new SNIHostName("test2.com");
        withConnectors(HTTPS, false, info -> {
            assertThat(info.getConnectors())
                .containsOnlyKeys(HTTPS);
            try {
                assertThat(client.newRequest(connector(HTTPS, info, HTTPS))
                    .headers(headers -> headers.add(HttpHeader.HOST, "test.com"))
                    .method(HttpMethod.GET)
                    .send()
                    .getStatus())
                    .isEqualTo(400);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void SslWithNullSNI() {
        sni = null;
        withConnectors(HTTPS, true, info -> {
            assertThat(info.getConnectors())
                .containsOnlyKeys(HTTPS);
            try {
                assertThat(client.newRequest(connector(HTTPS, info, HTTPS))
                    .headers(headers -> headers.add(HttpHeader.HOST, "test.com"))
                    .method(HttpMethod.GET)
                    .send()
                    .getContentAsString())
                    .isEqualTo("boop");
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void SslWithNullSNIShouldFailIfEmptySniNotAllowed() {
        sni = null;
        withConnectors(HTTPS, false, info -> {
            assertThat(info.getConnectors())
                .containsOnlyKeys(HTTPS);
            try {
                assertThat(client.newRequest(connector(HTTPS, info, HTTPS))
                    .headers(headers -> headers.add(HttpHeader.HOST, "test.com"))
                    .method(HttpMethod.GET)
                    .send()
                    .getStatus())
                    .isEqualTo(400);
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String connector(String protocol, HttpServerInfo info, String name) {
        return String.format("%s://localhost:%s/beep", protocol, info.getConnectors().get(name).getPort());
    }

    private void withConnectors(String connectorList, boolean allowEmptySni, Consumer<HttpServerInfo> action) {
        final Map<String, Object> props = ImmutableMap.<String, Object>builder()
            .put("PORT0", 32743)
            .put("PORT1", 32744)
            .put("ot.httpserver.connector.secure-http.forceSecure", true)
            /*
             adapted from SslContextFactory.DEFAULT_EXCLUDED_CIPHER_SUITES
             EXCLUDED_CIPHER_SUITES = {
                // Exclude weak / insecure ciphers
                "^.*_(MD5|SHA|SHA1)$",
                // Exclude ciphers that don't support forward secrecy
                "^TLS_RSA_.*$" ....
             */
            .put("ot.httpserver.ssl-excluded-cipher-suits", "" +
                "^.*_(MD5|SHA1)$," +                          // we allow ciphers with SHA$, will do selective exclusion
                "^TLS_ECDHE_ECDSA_WITH_AES_.*SHA$," +         // exclude SHA$
                "^TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA$," +     // exclude SHA$
                "^TLS_ECDH_.*_SHA$," +                        // exclude SHA$
                "^TLS_DHE_.*_SHA$," +                         // exclude SHA$
                "^TLS_RSA_WITH_AES_128_CBC_SHA256$," +        // jetty excludes anything with "^TLS_RSA_.*$",
                "^TLS_RSA_WITH_AES_128_GCM_.*$," +            // exclude ^TLS_RSA
                "^TLS_RSA_WITH_AES_256_.*$," +                // exclude ^TLS_RSA
                "^SSL_.*$," +
                "^.*_NULL_.*$," +
                "^.*_anon_.*$")
            // the net effect of the above exclusions is that we end up allowing the desired
            // legacy ciphers through in particular
            // i)  TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA
            // ii) TLS_RSA_WITH_AES_128_CBC_SHA
            .put("ot.httpserver.connector.https.protocol", "https")
            .put("ot.httpserver.connector.https.keystore", keystore)
            .put("ot.httpserver.connector.https.keystorePassword", PASSWORD)
            .put("ot.httpserver.connector.https.allowEmptySni", allowEmptySni)
            .put("ot.httpserver.active-connectors", connectorList)
            .put("ot.jmx.port", 0)
            //.put("logging.level.org.eclipse.jetty", "TRACE")
            .build();
        try (ConfigurableApplicationContext ctx = OTApplication.run(TestingServer.class, new String[0], props)) {
            action.accept(ctx.getBean(HttpServerInfo.class));
        }
    }

    @MVCServer
    @Import(TestingServer.TestingResource.class)
    public static class TestingServer {

        @Bean
        ServiceInfo info() {
            return () -> "connector-test";
        }

        @RestController
        public static class TestingResource {

            @GetMapping("/beep")
            public String beep() {
                return "boop";
            }
        }
    }
}

