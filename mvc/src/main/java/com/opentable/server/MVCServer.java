
package com.opentable.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Mix-in Spring MVC
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
    // Pull mvc specific stuff
    MVCHttpServerCommonConfiguration.class,
})
// All the non specific mvc servlet stuff
@CoreHttpServerCommon
public @interface MVCServer {
}
