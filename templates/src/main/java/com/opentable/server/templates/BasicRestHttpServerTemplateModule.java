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

import com.google.inject.AbstractModule;
import com.palominolabs.metrics.guice.InstrumentationModule;

import com.opentable.config.Config;
import com.opentable.httpserver.HttpServerModule;
import com.opentable.httpserver.selftest.SelftestModule;
import com.opentable.jackson.OpenTableJacksonModule;
import com.opentable.jaxrs.OpenTableJaxRsServletModule;
import com.opentable.jaxrs.exceptions.OpenTableJaxRsExceptionMapperModule;
import com.opentable.jaxrs.json.OTJacksonJsonProvider;
import com.opentable.jmx.jolokia.JolokiaModule;
import com.opentable.scopes.threaddelegate.ThreadDelegatedScopeModule;
import com.opentable.serverinfo.ServerInfoModule;
import com.opentable.tracking.guice.TrackingModule;

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

        install (new InstrumentationModule());
        install (new JolokiaModule());

        install (new HttpServerModule(config));
        install (new OpenTableJaxRsServletModule(config, paths));
        install (new OpenTableJaxRsExceptionMapperModule());

        bind (OTJacksonJsonProvider.class);
        install (new OpenTableJacksonModule());

        install (new SelftestModule());
        install (new ServerInfoModule());
        install (new TrackingModule());
    }
}
