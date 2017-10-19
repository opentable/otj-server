package com.opentable.server;

import java.util.Map;
import java.util.function.Consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

/**
 * OpenTable specific Spring Boot style application runner.
 * Sets up logging and other OT specific customizations.
 *
 * Note that this API only accepts {@code Class<?>} arguments
 * rather than generic {@code Object} -- this is a style choice,
 * we prefer to write explicit classes rather than relying on instances.
 * This can change down the road if needed.
 */
public final class OTApplication {
    private OTApplication() { }
    /**
     * Construct and run a {@link SpringApplication} with the default settings for
     * {code otj-} OpenTable Spring Boot based applications.
     * @param applicationClass the main class to boot.  Should be a Spring configuration class.
     * @param args the {@code main()}-style application arguments
     * @return the configured application context
     */
    public static ConfigurableApplicationContext run(Class<?> applicationClass, String... args) {
        return run(applicationClass, args, b -> {});
    }

    /**
     * Construct and run a {@link SpringApplication} with custom settings for
     * {code otj-} OpenTable Spring Boot based applications.
     * @param applicationClass the main class to boot.  Should be a Spring configuration class.
     * @param args the {@code main()}-style application arguments
     * @param customize a hook to configure the application before running
     * @return the configured application context
     */
    public static ConfigurableApplicationContext run(Class<?> applicationClass, String[] args, Consumer<SpringApplicationBuilder> customize) {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
        final SpringApplicationBuilder builder = new SpringApplicationBuilder(applicationClass);
        builder.application().setMainApplicationClass(applicationClass);
        customize.accept(builder);
        return builder.run(args);
    }

    /**
     * Construct and run a {@link SpringApplication} with custom settings for
     * {code otj-} OpenTable Spring Boot based applications, accepting property overrides.
     * @param applicationClass the main class to boot.  Should be a Spring configuration class.
     * @param args the {@code main()}-style application arguments
     * @param properties a map of properties to override the standard environment-resolved properties
     * @param customize a hook to configure the application after property overrides, but before running
     * @return the configured application context
     */
    public static ConfigurableApplicationContext run(
            Class<?> applicationClass,
            String[] args,
            Map<String, Object> properties,
            Consumer<SpringApplicationBuilder> customize) {
        final Consumer<SpringApplicationBuilder> overrideProperties = builder ->
                builder.environment(
                        new StandardEnvironment() {
                            @Override
                            protected void customizePropertySources(MutablePropertySources propertySources) {
                                super.customizePropertySources(propertySources);
                                propertySources.addFirst(
                                        new MapPropertySource(
                                                "OTApplication.run$properties",
                                                properties
                                        )
                                );
                            }
                        }
                );
        return run(applicationClass, args, overrideProperties.andThen(customize));
    }

    /**
     * Construct and run a {@link SpringApplication} with custom settings for
     * {code otj-} OpenTable Spring Boot based applications, accepting property overrides.
     * @param applicationClass the main class to boot.  Should be a Spring configuration class.
     * @param args the {@code main()}-style application arguments
     * @param properties a map of properties to override the standard environment-resolved properties
     * @return the configured application context
     */
    public static ConfigurableApplicationContext run(
            Class<?> applicationClass,
            String[] args,
            Map<String, Object> properties) {
        return run(applicationClass, args, properties, b -> {});
    }
}
