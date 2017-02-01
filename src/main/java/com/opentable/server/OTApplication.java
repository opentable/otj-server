package com.opentable.server;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.opentable.spring.PropertySourceUtil;

/**
 * OpenTable specific Spring Boot style application runner.
 * Sets up logging and other OT specific customizations.
 *
 * Note that this API only accepts {@code Class<?>} arguments
 * rather than generic {@code Object} -- this is a style choice,
 * we prefer to write explicit classes rather than relying on instances.
 * This can change down the road if needed.
 */
public class OTApplication {
    private static final Logger LOG = LoggerFactory.getLogger(OTApplication.class);

    /**
     * Construct and run a {@link SpringApplication} with the default settings for
     * {code otj-} OpenTable Spring Boot based applications.
     * @param applicationClass the main class to boot.  Should be a Spring configuration class.
     * @param args the application arguments from main()
     * @return the configured application context
     */
    public static ConfigurableApplicationContext run(Class<?> applicationClass, String[] args) {
        return run(applicationClass, args, b -> {});
    }

    /**
     * Construct and run a {@link SpringApplication} with custom settings for
     * {code otj-} OpenTable Spring Boot based applications.
     * @param applicationClass the main class to boot.  Should be a Spring configuration class.
     * @param args the application arguments from main()
     * @return the configured application context
     */
    public static ConfigurableApplicationContext run(Class<?> applicationClass, String[] args, Consumer<SpringApplicationBuilder> customizer) {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        final SpringApplicationBuilder builder = new SpringApplicationBuilder(applicationClass);
        customizer.accept(builder);
        final ConfigurableApplicationContext ctx = builder.run(args);
        logProperties(ctx);
        return ctx;
    }

    private static void logProperties(final ConfigurableApplicationContext ctx) {
        LOG.info("logging resolved environment properties");
        PropertySourceUtil.getProperties(ctx.getEnvironment())
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(), // Key mapper.
                        Map.Entry::getValue,        // Value mapper.
                        (p1, p2) -> {
                            LOG.warn("duplicate resolved properties; picking first: {}, {}", p1, p2);
                            return p1;
                        }
                ))
                .entrySet()
                .stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> LOG.info("{}: {}", e.getKey(), e.getValue()));
    }
}
