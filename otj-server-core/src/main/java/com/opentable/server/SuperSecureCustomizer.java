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

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;

/**
 * Mark requests as secure independent of the transport we got them on.
 * Mostly useful when you are handling SSL traffic but an external
 * process terminated SSL for you.
 */
class SuperSecureCustomizer extends SecureRequestCustomizer {
    @Override
    public void customize(Connector connector, HttpConfiguration channelConfig, Request request) {
        super.customize(connector, channelConfig, request);
        if (!request.isSecure()) {
            super.customizeSecure(request);
        }
    }
}
