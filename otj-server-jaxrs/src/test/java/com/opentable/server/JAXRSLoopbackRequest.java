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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

@Named
// Copy paste and makes core even more redundant.
class JAXRSLoopbackRequest {
    private final Client client;
    private final Provider<HttpServerInfo> info;
    @Inject
    private JAXRSLoopbackRequest(@Named("test") Client client, Provider<HttpServerInfo> info) {
        this.client = client;
        this.info = info;
    }

    public WebTarget of(String path) {
        return client.target("http://localhost:" + info.get().getPort()).path(path);
    }
}
