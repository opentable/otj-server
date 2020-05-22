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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.AbstractConfiguration;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyWebServer;
import org.springframework.boot.web.servlet.ServletContextInitializer;

public class OTJettyServletWebServerFactory extends JettyServletWebServerFactory {

    private final Optional<Collection<Consumer<WebAppContext>>> webAppContextCustomizers;
    private final boolean showStacks;

    public OTJettyServletWebServerFactory(Optional<Collection<Consumer<WebAppContext>>> webAppContextCustomizers, boolean showStacks) {
        this.webAppContextCustomizers = webAppContextCustomizers;
        this.showStacks = showStacks;
    }

    public OTJettyServletWebServerFactory(int port) {
        super(port);
        this.webAppContextCustomizers = Optional.empty();
        this.showStacks = true;
    }

    public OTJettyServletWebServerFactory(String contextPath, int port) {
        super(contextPath, port);
        this.webAppContextCustomizers = Optional.empty();
        this.showStacks = true;
    }

    public OTJettyServletWebServerFactory() {
        super();
        this.webAppContextCustomizers = Optional.empty();
        this.showStacks = true;
    }

    @Override
    protected Configuration[] getWebAppContextConfigurations(WebAppContext webAppContext,
                                                             ServletContextInitializer... initializers) {
        List<Configuration> res = new ArrayList<>(Arrays.asList(super.getWebAppContextConfigurations(webAppContext, initializers)));
        res.add(new AbstractConfiguration() {
            @Override
            public void configure(WebAppContext context) {
                webAppContextCustomizers.ifPresent(consumers -> consumers.forEach(c -> c.accept(context)));
            }
        });
        res.add(new AbstractConfiguration() {
            @Override
            public void configure(WebAppContext context) {
                context.setErrorHandler(new ConservedHeadersJettyErrorHandler(context.getErrorHandler(), showStacks));
            }
        });
        return res.toArray(new Configuration[0]);
    }

    @Override
    protected JettyWebServer getJettyWebServer(Server server) {
        // always auto-start even if the default connector isn't configured
        return new JettyWebServer(server, true);
    }
}
