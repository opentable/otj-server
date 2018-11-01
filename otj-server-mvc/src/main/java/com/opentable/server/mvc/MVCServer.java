
package com.opentable.server.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.server.CoreHttpServerCommon;

/**
 * Mix-in Spring MVC
 */
@Configuration
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
    // Pull MVC specific stuff
    MVCHttpServerCommonConfiguration.class,
})
// All the non specific MVC servlet stuff
@CoreHttpServerCommon
public @interface MVCServer {
}
