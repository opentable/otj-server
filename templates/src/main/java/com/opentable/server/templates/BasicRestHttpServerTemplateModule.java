/**
 * Copyright (C) 2012 Ness Computing, Inc.
 *
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
package com.opentable.server.templates;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;

import com.opentable.config.Config;
import com.opentable.conservedheaders.ClientConservedHeadersFeature;
import com.opentable.conservedheaders.guice.ConservedHeadersModule;
import com.opentable.httpserver.HttpServerModule;
import com.opentable.jackson.OpenTableJacksonModule;
import com.opentable.jaxrs.JaxRsClientBinder;
import com.opentable.jaxrs.OpenTableJaxRsServletModule;
import com.opentable.jaxrs.StandardFeatureGroup;
import com.opentable.jaxrs.exceptions.OpenTableJaxRsExceptionMapperModule;
import com.opentable.jaxrs.json.OTJacksonJsonProvider;
import com.opentable.jmx.JmxServerModule;
import com.opentable.jmx.jolokia.JolokiaModule;
import com.opentable.metrics.DefaultMetricsModule;
import com.opentable.metrics.http.HealthHttpModule;
import com.opentable.scopes.threaddelegate.ThreadDelegatedScopeModule;
import com.opentable.serverinfo.ServerInfoModule;

/**
 * Defines a basic server suitable for serving REST resources using JSON over HTTP.
 *
 * <ul>
 *   <li>Codahale metrics</li>
 *   <li>Jolokia JMX access over HTTP</li>
 *   <li>JDBI database configuration</li>
 *   <li>JAX-RS with exception handling</li>
 *   <li>selftest endpoint</li>
 * </ul>
 */
public class BasicRestHttpServerTemplateModule extends AbstractModule
{
    private final Config config;
    private final String[] paths;

    public BasicRestHttpServerTemplateModule(final Config config)
    {
        this(config, "/*");
    }

    public BasicRestHttpServerTemplateModule(final Config config, final String... paths)
    {
        this.config = config;
        this.paths = paths;
    }

    @Override
    protected void configure()
    {
        install (new ThreadDelegatedScopeModule());

        // TODO Perhaps place these into a separate module?
        final MetricRegistry metricRegistry = new MetricRegistry();
        bind(MetricRegistry.class).toInstance(metricRegistry);
        install (new MetricsInstrumentationModule(metricRegistry));
        bind(HealthCheckRegistry.class).toInstance(new HealthCheckRegistry());

        install (new JmxServerModule());
        install (new JolokiaModule());

        install (new HttpServerModule(config));

        bind (OTJacksonJsonProvider.class);
        install (new OpenTableJacksonModule());

        install (new DefaultMetricsModule());
        install (new HealthHttpModule());
        install (new ServerInfoModule());
        install (new ConservedHeadersModule());
        JaxRsClientBinder.bindFeatureToGroup(binder(), StandardFeatureGroup.PLATFORM_INTERNAL)
            .to(ClientConservedHeadersFeature.class);

        install (new OpenTableJaxRsServletModule(config, paths));
        install (new OpenTableJaxRsExceptionMapperModule());
    }
}
