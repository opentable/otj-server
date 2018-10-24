package com.opentable.server;

import static org.junit.Assert.assertEquals;

import java.util.SortedMap;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

public class TestServerMetrics extends AbstractTest {

    @Autowired private TestRestTemplate testRestTemplate;
    @Autowired private MetricRegistry metricRegistry;

    @Test
    public void testRequestMetrics() throws InterruptedException {
        SortedMap<String, Meter> meters = metricRegistry.getMeters();
        Meter okResponseMeter = meters.get("http-server.2xx-responses");
        Meter notFoundResponseMeter = meters.get("http-server.4xx-responses");
        long currentOk = okResponseMeter.getCount();
        long currntNotFound = notFoundResponseMeter.getCount();
        for (int i = 0; i < 123; i++) {
            testRestTemplate.getForObject("/api/test", String.class);
        }
        for (int i = 0; i < 51; i++) {
            testRestTemplate.getForObject("/totally/fake", String.class);
        }
        Thread.sleep(2000l);
        assertEquals(123, okResponseMeter.getCount() - currentOk);
        assertEquals(51, notFoundResponseMeter.getCount() - currntNotFound);
    }
}

