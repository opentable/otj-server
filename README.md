OpenTable Server Component
==========================

Component Charter
-----------------

* Server startup and shutdown
* Templates that integrate components into useful starting points for service development

Server Main Class
-----------------

`otj-server` provides [OTApplication](https://github.com/opentable/otj-server/blob/master/server/src/main/java/com/opentable/server/OTApplication.java)
which does our initialization and then invokes `SpringApplication.run` to actually boot the service.

The [@RestHttpServer](https://github.com/opentable/otj-server/blob/master/server/src/main/java/com/opentable/server/RestHttpServer.java)
configuration provides basic necessities for running a web service:

* Jetty HTTP server
* RESTEasy JAX-RS runtime
* OT Conserved Headers
* `otj-metrics` integration
* `otj-jackson` integration
* JVM pause detector
* JMX monitoring and management
* Static resource serving over HTTP
* CORS header support
* Logging configuration

Jetty Configuration
-------------------

At first take, this might seem like something Spring Boot would provide for you -- but unfortunately the
situation at the time of this writing is that the Spring Boot integration is quite simplistic and does not allow
you to customize many important Jetty features, like graceful shutdown or additional HTTPS connectors with a custom keystore.

The Boot team is tracking
[various](https://github.com/spring-projects/spring-boot/issues/4657)
[improvements](https://github.com/spring-projects/spring-boot/issues/5314)
but until those ship we will maintain our own code.

This doesn't seem to be improving meaningfully for Spring Boot 2.0 which is disappointing.

We add support for declaring named connectors in configuration, and then
choosing from those as a set of active connectors.

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

The `boot` connector is behaving like `default-http` connector, but takes host/port from Spring Boot's default connector.


Previous versions of `otj-server` had a configurable `ot.http.bind-port`; this usually would be replaced with e.g.
```
ot.httpserver.connector.default-http.port=8080
```

Usually the defaults are okay.  You might tune your thread pool size for heavily utilized services.  See more detail on the wiki, under [Jetty HTTP Server](https://wiki.otcorp.opentable.com/display/CP/Jetty+HTTP+Server#JettyHTTPServer-ThreadPoolUsage).

```
ot.httpserver.max-threads=32
```

JMX Configuration
-----------------

```
ot.jmx.port=12345
ot.jmx.address=127.0.0.1
ot.jmx.url-format=service:jmx:jmxmp://%s:%s
```

----
Copyright (C) 2016 OpenTable, Inc.
