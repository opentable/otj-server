package com.opentable.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;

import com.opentable.metrics.http.MetricsHttpConfiguration;

/**
 * Demonstration of serving Dropwizard Metrics AdminServlet.
 * Navigate to /metrics.
 * Uncomment in {@link #testProperties()} to test Graphite reporting.
 * Note that if you want to test NMT metrics, you will need to add {@code -XX:NativeMemoryTracking=summary}
 * to your JVM arguments.
 */
@Configuration
@Import({
        TestServer.class,
        MetricsHttpConfiguration.class,
})
public class MetricsDemo {
    public static void main(final String[] args) {
        final SpringApplication app = new SpringApplication(MetricsDemo.class);
        app.setDefaultProperties(testProperties());
        app.run(args);
    }

    private static Map<String, Object> testProperties() {
        final Map<String, Object> props = new HashMap<>();
        props.put("INSTANCE_NO", "0");
        props.put("OT_ENV_TYPE", "dev");
        props.put("OT_ENV_LOCATION", "sf");
        //props.put("ot.graphite.graphite-host", "carbon-qa-sf.otenv.com");
        return Collections.unmodifiableMap(props);
    }
}
