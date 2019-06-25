# OpenTable Server Component


The OpenTable Java Server component is the main code entry point into the OpenTable Java (OTJ) stack. It provides a customized Spring Boot set up. The customizations are numerous and include:

 * Graceful shutdown
 * Jetty HTTP server
 * RESTEasy JAX-RS runtime
 * OT Conserved Headers
 * `otj-metrics` integration
 * `otj-jackson` integration
 * JVM pause detector
 * JMX monitoring and management
 * Static resource serving over HTTP
 * Logging configuration

## Flavors

There are 3 flavors of OTJ Server available:
 * **JAX-RS** - this uses RestEasy to create web services and as a REST client. RestEasy is an implementation of the Java API for RESTful Web Services (JAX-RS) specification.
 * **MVC** - this uses Spring's Model View Controller (MVC) framework to create web services. For a REST client with this flavor, we recommend otj-rest-template.
 * **Reactive** - this uses Spring's WebFlux reactive framework to create reactive web services. For a REST client with this flavor, we recommend otj-webclient.
 
## Differences Between Flavors

For the most part we expect the servers to act the same. One difference is how we handle CORS headers. In JAX-RS we send CORS headers for all requests. In Spring MVC and WebFlux you need to add a `@CrossOrigin` header to the controller when needed.

### Modules

There are 4 modules in this project, the core module is for code shared in common between both flavors.

## Getting Started

Much like Spring boot, you need to create an Application or Main class. There are examples below. You can also use our Maven archetype to create a new project from our template, see https://wiki.otcorp.opentable.com/display/PA/Create+a+New+Project+from+an+Archetype for more information.

## Examples

We have examples projects that show how to use server:
 - https://github.com/opentable/service-demo (JAX-RS)
 - https://github.com/opentable/service-otj-mvc-demo (Spring MVC)
  
### JAX-RS Example

```java
package com.opentable;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.opentable.server.JAXRSServer;
import com.opentable.server.OTApplication;
import com.opentable.service.ServiceInfo;
import com.opentable.service.discovery.client.EnableDiscoveryClient;

@Configuration
@JAXRSServer // This enabled the JAX-RS server
@EnableDiscoveryClient // This enables the discovery client
@Import(DemoServerConfiguration.class) // Imports a configuration class, needed as component scanning is disabled by default
public class DemoServerMain
{
    /**
     * Standard Java entry point.  Almost all of the real work is done in the
     * {@link JAXRSServer}.
     */
    public static void main(String[] args)
    {
        OTApplication.run(DemoServerMain.class, args); // Starts the application, equivalent to SpringApplication
    }

    @Bean
    public ServiceInfo serviceInfo() { // Every service should create a service info bean
        return new ServiceInfo() {
            @Override
            public String getName() {
                return "demo-server"; // used for discovery name, metrics, etc.
            }
        };
    }
}
```


### Spring MVC Example

```java
package com.opentable.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.opentable.server.MVCServer;
import com.opentable.server.OTApplication;
import com.opentable.service.ServiceInfo;
import com.opentable.service.discovery.client.EnableDiscoveryClient;

@Configuration
@MVCServer // Configures the server with Spring MVC
@EnableDiscoveryClient // Enable the discovery client
@ComponentScan // Enable component scanning
public class DemoServerMain {

    /**
     * Standard Java entry point.  Almost all of the real work is done in the
     * {@link MVCServer}.
     */
    public static void main(String[] args)
    {
        OTApplication.run(DemoServerMain.class, args); // Starts the application, equivalent to SpringApplication
    }

    @Bean
    public ServiceInfo serviceInfo() {
        return new ServiceInfo() {
            @Override
            public String getName() {
                return "otj-mvc-demo"; // used for discovery name, metrics, etc.
            }
        };
    }
}
```


### Spring WebFlux Reactive Example
```java
package com.opentable.reactivedemo;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
 
import com.opentable.server.OTApplication;
import com.opentable.server.reactive.ReactiveServer;
import com.opentable.service.ServiceInfo;
import com.opentable.service.discovery.client.EnableDiscoveryClient;
 
@Configuration
@ReactiveServer
@EnableDiscoveryClient
public class ReactiveDemoApplication {
 
    @Value("${ot.service.name:service-reactive-demo}")
    private String serviceName;
 
    public static void main(String[] args) {
        OTApplication.run(ReactiveDemoApplication.class, args);
    }
 
    @Bean
    public ServiceInfo serviceInfo() {
        return () -> serviceName;
    }
}
```

## Server Main Class

`otj-server` provides [OTApplication](https://github.com/opentable/otj-server/blob/master/server/src/main/java/com/opentable/server/OTApplication.java)
which does our initialization and then invokes `SpringApplication.run` to actually boot the service.

The `@JAXRSServer`, `@MVCServer`, `@ReactiveServer` annotations provide the basic necessities for running a web service:


### Jetty Configuration

We setup an embedded Jetty servlet container with many customizations like graceful shutdown and additional HTTPS connectors with a custom keystore. We also instrument the container and report metrics from it.

These customizations are not possible to do using just Spring Boot. We are monitoring Spring Boot and if it changes we will switch to use Spring Boot directly when possible.

Here are a couple of the Spring Boot issues we are tracking:
[Issue #4657](https://github.com/spring-projects/spring-boot/issues/4657)
[Issue #5314](https://github.com/spring-projects/spring-boot/issues/5314)

### Named HTTP Connectors

We add support for declaring named connectors in configuration, and then choosing from those as a set of active connectors.

Each connector has properties `port`, `protocol`, `forceSecure`, `keystore`, and `keystorePassword`.

`port` defaults to `-1`, which indicates to take the next port injected via
[PORT0, PORT1, ...].  `0` means to assign any available port.  Any positive
number requests that port literally.

Note that currently it is your responsibility to ensure the number of assigned
ports meshes with your configuration of e.g. JMX port.  We might improve this in the future.

`protocol` should be one of `http`, `https`, `proxy+http`, or `proxy+https`.

`forceSecure` should be set on connectors that are *not already secure* (i.e., never on a `https` connector)
but are terminated securely elsewhere.  You might use this if F5 terminates SSL in front of Frontdoor, for example.

`keystore` declares a path to a Java keystore to use for SSL.

```
# first, declare all your connectors
## default-http is usually on $PORT0
ot.httpserver.connector.default-http.port=-1
## you could declare a fixed-http connector, this is useful to e.g. connect direct to ELB
## but it'll cause deployment problems - usually you'd only do this in development
ot.httpserver.connector.fixed-http.port=8080
## and maybe you want to secure some things with TLS
## port defaults to -1 so this will get $PORT1
ot.httpserver.connector.my-https.protocol=https              # this connector is https
ot.httpserver.connector.my-https.keystore=/some/keystore.jks # and has these keys loaded

# activate connectors.  connectors declared but not referenced here are inactive
# particularly note that default-http needs to be here if you want it active
ot.httpserver.active-connectors=default-http,fixed-http,my-https
```

The `default-http` connector is hard-wired to Spring Boot's default connector and is less customizable;
the rest are created by the `otj-server` code and wired to Jetty ourselves.

The `boot` connector is the same as the  `default-http` connector, but it takes its host and port from Spring Boot's default connector.

## Configuration

Previous versions of `otj-server` had a configurable `ot.http.bind-port`; this usually would be replaced with e.g.
```
ot.httpserver.connector.default-http.port=8080
```

Usually the defaults are okay.  You might tune your thread pool size for heavily utilized services.  See more detail on the wiki, under [Jetty HTTP Server](https://wiki.otcorp.opentable.com/display/CP/Jetty+HTTP+Server#JettyHTTPServer-ThreadPoolUsage).

```
ot.httpserver.max-threads=32
```

### JMX Configuration

```
ot.jmx.port=12345
ot.jmx.address=127.0.0.1
ot.jmx.url-format=service:jmx:jmxmp://%s:%s
```
## Migration from 2.0.0

JaxRS users (if you were using OTJ stack before then this is *probably* you):

- Remove otj-server dependency and add `otj-server-core` & `otj-server-jaxrs` dependencies
- Change annotation from `@RestHttpServer` to `@JaxRSServer`

MVC users:

- Remove `otj-server` dependency and add `otj-server-core` & `otj-server-mvc` dependencies
- Change annotation from `@RestHttpServer` to `@MVCServer`

If you want to use both:
- Use both annotations!
- Use `otj-server-core`, `otj-server-mvc`, `otj-server-jaxrs`

----
Copyright (C) 2018 OpenTable, Inc.
