package com.opentable.server.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.reactive.config.DelegatingWebFluxConfiguration;

/**
 * Configures Spring WebFlux reactive server.
 *
 * Uses {@link DelegatingWebFluxConfiguration} to customize the configuration by delegating
 * to beans of type {@link org.springframework.web.reactive.config.WebFluxConfigurer}, allowing them to
 * customize the configuration provided by {@code WebFluxConfigurationSupport}.
 *
 * See {@link ReactiveServerUnconfigured} for more advanced control of configuration.
 *
 * This annotation is generally equivalent to {@link org.springframework.web.reactive.config.EnableWebFlux}.
 *
 * TODO: Look at @ImportAutoConfiguration class?
 *
 * https://spring.io/blog/2019/01/21/manual-bean-definitions-in-spring-boot
 *
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        DelegatingWebFluxConfiguration.class,
})
@ReactiveServerUnconfigured
public @interface ReactiveServer {
}
