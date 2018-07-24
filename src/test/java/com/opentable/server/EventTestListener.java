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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.event.EventListener;


public class EventTestListener {
    private static final Logger LOG = LoggerFactory.getLogger(EventTestListener.class);
    private final HttpServerInfo info;


    @Inject
    public EventTestListener(HttpServerInfo info) {
        this.info = info;
    }

    @EventListener
    public void containerInitialized(final WebServerInitializedEvent evt) {
        // Should blow up if other didn't first
        LOG.debug("I like ports " + info.getPort());
        LOG.debug("EventTestListener received container initialized event: {}", evt);
    }
}
