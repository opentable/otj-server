package com.opentable.server.reactive;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * More advanced control for configuring Spring WebFlux.
 *
 * Users of this annotation must either directly import
 * {@link org.springframework.web.reactive.config.WebFluxConfigurationSupport}, or else
 * extend it in a @Configuration class and override its protected methods to provide advanced configuration.
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        ReactiveServerCommonConfiguration.class
})
public @interface ReactiveServerUnconfigured {
}
