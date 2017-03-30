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

We expose a few important configuration options for Jetty:

```
ot.http.bind-port=8080,8081
ot.httpserver.shutdown-timeout=PT1m
ot.httpserver.max-threads=200
```

Usually the defaults are okay.  You might tune your thread pool size for heavily utilized services.  See more detail on the wiki, under [Jetty HTTP Server](https://wiki.otcorp.opentable.com/display/CP/Jetty+HTTP+Server#JettyHTTPServer-ThreadPoolUsage).

JMX Configuration
-----------------

```
ot.jmx.port=12345
ot.jmx.address=127.0.0.1
ot.jmx.url-format=service:jmx:jmxmp://%s:%s
```

----
Copyright (C) 2016 OpenTable, Inc.
