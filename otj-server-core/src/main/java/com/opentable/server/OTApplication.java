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

import java.net.URI;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
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

    private static final String OPT_IN_TO_NEW_BEHAVIOR = "ot.spring.logging";
    public static final String ORG_SPRINGFRAMEWORK_BOOT_LOGGING_LOGGING_SYSTEM = "org.springframework.boot.logging.LoggingSystem";
    public static final String LOGGING_CONFIG = "logging.config";

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
        /*
         * Truth table of scenarios
         *
         * 1. New Server, New otpl-common-config
         * Goal: We'd want this to switch over, conditionally to the new system.
         * Answer: The code below, plus otpl-common-config adding the OPT_IN_TO_NEW_BEHAVIOR and the LOGGING_CONFIG. otpl-common-config never sets
         * the logging system directly.
         *
         * 2. New Server, old otpl-common-config
         * Goal: We'd want this to have the old behavior, eg turn off spring logging
         * Answer: The code below will continue to execute old path unless user manually sets OPT_IN_TO_NEW_BEHAVIOR and the LOGGING_CONFIG.
         * This also covers java8/9/10 which we won't update otpl-common-config for.
         *
         * 3. Old Server, New otpl-common-confiig
         * Goal: We'd want this to "work". Either option is acceptable
         * Answer: The old otj-server ignores these switches, so assuming it's safe to set LOGGING_CONFIG but set none, we are fine
         *
         * 4. Old Server, Old otpl-common-config
         * Goal: By definition old Spring system
         * Answer: By definition this works
         *
         * 5. Consider these running locally, without otpl-common-config
         * Answer: Unless they manually sets OPT_IN_TO_NEW_BEHAVIOR and the LOGGING_CONFIG, the old behavior applies
         *
         * In addition, scenario 1 has an escape hatch, the user can manually set this and opt out.
         */
        final boolean enableSpringLogging = Boolean.parseBoolean(System.getProperty(OPT_IN_TO_NEW_BEHAVIOR));
        final boolean springLoggingConfigFileExists = StringUtils.isNotBlank(System.getProperty(LOGGING_CONFIG));
        // In these cases, use old behavior
        if (!enableSpringLogging || !springLoggingConfigFileExists) {
            System.setProperty(ORG_SPRINGFRAMEWORK_BOOT_LOGGING_LOGGING_SYSTEM, "none"); // Disable spring system
            System.clearProperty(LOGGING_CONFIG); // Remove since it shouldn't be in use
        }
        final SpringApplicationBuilder builder = new SpringApplicationBuilder(applicationClass);
        builder.main(applicationClass);
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

    /**
     * @param ctx Spring application context
     * @return URI Return the base URI for a given application context.  Mostly useful in tests.
     */
    public static URI getBaseUri(ConfigurableApplicationContext ctx) {
        return getBaseUri(ctx, "default");
    }

    /**
     * @param ctx Spring application context
     * @param connector connector name. Currently only default is accepted.
     * @return URI Return the base URI for a given application context.  Mostly useful in tests.
     */
    public static URI getBaseUri(ConfigurableApplicationContext ctx, String connector) {
        Preconditions.checkState("default".equals(connector), "TODO: implement non-default connectors");
        return URI.create("http://127.0.0.1:" + ctx.getBean(HttpServerInfo.class).getPort());
    }
}
