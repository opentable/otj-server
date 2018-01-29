package com.opentable.server;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * REST Reactive HTTP Server.
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
    EmbeddedReactiveJetty.class,
    RestHttpServerCommon.class
})
public @interface RestReactiveHttpServer {
}
