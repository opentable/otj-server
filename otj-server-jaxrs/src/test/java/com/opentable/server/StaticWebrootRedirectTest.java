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
        try(final Response r = request
                .of("")
                .property(JaxRsClientProperties.FOLLOW_REDIRECTS, false)
                .request()
                .get()){
            Assertions.assertThat(r.getStatus()).isEqualTo(307);
            Assertions.assertThat(r.getLocation().getPath()).isEqualTo(String.format("/%s/index.html", path));
        }
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
