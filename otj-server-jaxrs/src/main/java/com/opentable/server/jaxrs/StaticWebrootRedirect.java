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
package com.opentable.server.jaxrs;

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.opentable.server.StaticResourceConfiguration;

@Path("/")
public class StaticWebrootRedirect {
    private static final String INDEX_FILE_NAME = "index.html";
    private final URI location;

    public StaticWebrootRedirect() {
        this(URI.create(String.format("/%s/%s", StaticResourceConfiguration.DEFAULT_PATH_NAME, INDEX_FILE_NAME)));
    }

    @Inject
    public StaticWebrootRedirect(final StaticResourceConfiguration config) {
        this(URI.create(config.staticPath(INDEX_FILE_NAME)));
    }

    public StaticWebrootRedirect(URI location) {
        this.location = location;
    }

    @GET
    public Response redirect() {
        return Response.temporaryRedirect(location).build();
    }
}
