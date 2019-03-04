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
package com.opentable.server.reactive;

import static org.junit.Assert.assertEquals;

import java.util.SortedMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

public class TestServerMetrics extends AbstractTest {

    @Autowired private WebTestClient webTestClient;
    @Autowired private MetricRegistry metricRegistry;

    @Test
    public void testRequestMetrics() throws InterruptedException {
        SortedMap<String, Meter> meters = metricRegistry.getMeters();
        Meter okResponseMeter = meters.get("http-server.2xx-responses");
        Meter notFoundResponseMeter = meters.get("http-server.4xx-responses");
        Counter activeSuspended = metricRegistry.getCounters().get("http-server.active-suspended");
        Timer requests = metricRegistry.getTimers().get("http-server.requests");
        Timer dispatches = metricRegistry.getTimers().get("http-server.dispatches");

        long currentOk = okResponseMeter.getCount();
        long currentNotFound = notFoundResponseMeter.getCount();
        long currentSuspended = activeSuspended.getCount();
        long currentRequests = requests.getCount();
        long currentDispatches = dispatches.getCount();

        for (int i = 0; i < 123; i++) {
            webTestClient.get().uri("/api/test").exchange().expectBody(String.class).returnResult();
        }
        for (int i = 0; i < 51; i++) {
            webTestClient.get().uri("/totally/fake").exchange().expectBody(String.class).returnResult();
        }
        Thread.sleep(2000l);
        assertEquals(123, okResponseMeter.getCount() - currentOk);
        assertEquals(51, notFoundResponseMeter.getCount() - currentNotFound);
        assertEquals(0, activeSuspended.getCount() - currentSuspended);
        assertEquals(174, requests.getCount() - currentRequests);
        assertEquals(174, dispatches.getCount() - currentDispatches);
    }

    @Test
    public void testAsyncRequestMetrics() throws InterruptedException {
        SortedMap<String, Meter> meters = metricRegistry.getMeters();
        Meter okResponseMeter = meters.get("http-server.2xx-responses");
        Meter notFoundResponseMeter = meters.get("http-server.4xx-responses");
        Counter activeSuspended = metricRegistry.getCounters().get("http-server.active-suspended");
        Timer requests = metricRegistry.getTimers().get("http-server.requests");
        Timer dispatches = metricRegistry.getTimers().get("http-server.dispatches");

        long currentOk = okResponseMeter.getCount();
        long currentNotFound = notFoundResponseMeter.getCount();
        long currentSuspended = activeSuspended.getCount();
        long currentRequests = requests.getCount();
        long currentDispatches = dispatches.getCount();

        for (int i = 0; i < 10; i++) {
            webTestClient.get().uri("/api/async").exchange().expectBody(String.class).returnResult();
        }
        for (int i = 0; i < 5; i++) {
            webTestClient.get().uri("/totally/fake").exchange().expectBody(String.class).returnResult();
        }
        Thread.sleep(2000l);
        assertEquals(10, okResponseMeter.getCount() - currentOk);
        assertEquals(5, notFoundResponseMeter.getCount() - currentNotFound);
        assertEquals(0, activeSuspended.getCount() - currentSuspended);
        assertEquals(15, requests.getCount() - currentRequests);
        assertEquals(15, dispatches.getCount() - currentDispatches);
    }
}
