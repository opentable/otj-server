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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

@ManagedResource
public class JettyDumper {

    private final Provider<Server> jetty;

    @Inject
    JettyDumper(Provider<Server> jetty) {
        this.jetty = jetty;
    }

    @ManagedOperation
    public String dumpJetty() throws IOException {
        final StringBuilder dump = new StringBuilder();
        jetty.get().dump(dump, "  ");
        final String result = dump.toString();
        LoggerFactory.getLogger(JettyDumper.class).info("Jetty Internal State\n{}", result);
        return result;
    }
}
