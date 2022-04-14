otj-server
=========

5.2.13
------
* Parent POM-328 [changes see here]( https://github.com/opentable/otj-parent/blob/master/CHANGELOG.md#328)

5.2.10
------
* Add connection limit option. Combined with the Low Resource Monitor, may be a useful way to protect your service.

5.2.9
-----
* Remove JAXRS (phase I - comment out modules)

5.2.7
----
* Add ot.server.max-request-size (default = 16384) to set the maximum header size. You
may change as desired, a value of <=0 keeps the Jetty default (8192).
  
5.2.6
-----
* Add ot.server.backend.info.enabled to suppress backend filter info
* Remove all usage of JAXRS in test scope of core.

5.2.4
-----
* POM 267
* Recompile against otj-logging 5.2.2
* Remove many careless dep pins

5.2.3
=====
* update to parent pom 264
* deleted SuperSadSslFactory
* introduced ot.httpserver.ssl-excluded-cipher-suits flag

5.2.2
=====
* Jetty low resource monitor. 
* ability to configure IdleTimeout on the connector. 

5.2.1
=====
* Remove jax rs api from otj-server.core. The otj-server-jaxrs still
provides this, so issues will be limited to folks using uribuilder without jaxrs, and
they should get a compilation error and switch. their usage
was fragile anyway, depending, on a full jaxrs dep import.

5.2.0
------
* Recompile for Spring 5.2

3.1.5
-----
* convenience method in OTApplication`commandline`, making
it easier to implement command line or scheduled job services without
a web servlet.
* Switch to spring logging if `ot.spring.logging` is set
to true.

3.1.4
----
* @ReactiveServer using otj-jackson configuration
* Refactoring of boot connnector

3.1.1, 3.1.2, 3.1.3
----
* Minor fixes to port selector

3.1.0
-----
* Significant refactoring to provide support for Named Ports in Kubernetes. This may
have some corner-case effects in Singularity as well. See `Port selector notes` in the README.


3.0.23
------
* K8s logging

3.0.22
------
* Fixes Spring Boot white error page handler for all HTTP methods

3.0.21
------
* Fixes for Spring Boot changing order of configuration

3.0.20
------
* Revert "OTApplication: Do not set `org.springframework.boot.logging.LoggingSystem=none` property on startup."

3.0.19
------
* OTApplication: Do not set `org.springframework.boot.logging.LoggingSystem=none` property on startup.
* Remove service security filter installation so that it will be handled as a manual process by each backend server. Avoids
a circular dependency issue for certain platform libraries. Filters must be manually installed directly in backend servers
by importing the relevant config class. See otj-service-security README for more details.

3.0.18
------
* Adds Service Security filters to jaxrs, mvc, and reactive servers (disabled by default)

3.0.17
------
* JettyReactiveWebServerFactory implementation moved to the static class, to avoid Actuator initialization problems.

3.0.16
------
* Obey HttpBlacklist in ConservedHeadersJettyErrorHandler

3.0.15
------
* Recompile for Resteasy 4.1.1

3.0.14
------
* Bug fixes on ready check.

3.0.13
-----
* Remove deprecated `ServerRequestLog` class. Reactive server logging is now handled entirely by the standard `JsonRequestLog`.
* Integrate ReadyCheck system.

3.0.12
-----
* ConservedHeadersJettyErrorHandler preserves conserved headers for unhandled servlet exceptions.
* Jetty default error response for 5xx is modified to disable printing of the full stack trace. It can be explicitly enabled
by property `ot.httpserver.show-stack-on-error=true`

3.0.11
-----
* Supports conserved headers for reactive Spring WebFlux applications (server-side, not for WebClient which will be
handled as part of the upcoming otj-webclient library). Uses otj-conservedheaders-reactive 3.0.2.
- Copies conserved headers to the MDC for application and server request logging
- Copies conserved headers to the outgoing request
- Creates a request-id if none was provided on incoming headers

3.0.10
-----
* Make constructor for otj-sever-reactive's ServerRequestLog public, to support request-id logging for reactive
servers who override JsonRequestLog with their own OTLs. *NOTE: This will be deprecated once otj-conservedheaders-reactive
is released, which will provide a more official solution.*

3.0.9
-----
* ThreadNameFilter - changes thread name executing servlet chain to the `<timestamp>:HttpServletRequest::getRequestURI()`
Can be disabled by configuration parameter `ot.server.thread-name-filter=false`
* ExceptionLogFilter - provides additional exception logging. This avoids losing valuable MDC information. 
Can be disabled by configuration parameter `ot.server.exception-log-filter=false`
* SECURITY: No longer sends Server: Jetty version header.

3.0.8
-----
* Added configuration parameter `ot.httpserver.ssl-excluded-protocols`  
To address Java 11's problematic implementation of TLS 1.3

3.0.7
-----
* Split Reactive Server in to its own top-level server component
* Replace @RestReactiveHttpServer with @ReactiveServer annotation

3.0.6
-----
* Fix EmbeddedReactiveJetty to work after module split (fixes regression post pom 180).

3.0.5
-----
* In reaction to the issues with 16 cores, log processors.
* Remove xml mapping for otj-server-mvc that would throw exception

3.0.4
-----
* Rebuild for DAG
* otj-filterorder 0.0.5
* Substantive rework of TestMBeanServerConfiguration to support SBT 2.11 properly

3.0.3
-----
* Clean up internal dependency declarations
* Dropwizard 4 recompile
c
3.0.2
-----
* Uses otj-filterorder 0.0.4

3.0.1
-----
* OTPL-3010 - fix/workaround for "java.lang.IllegalArgumentException: CBORFactory required"

3.0.0
-----
* Split into multiple modules: core for shared logic and separate modules for MVC and JAX-RS
* Support for Spring MVC!

2.15.6
------
* application.properties => otj-server.properties, preventing dep clash
* filterorder 0.0.3
* Allow configuration of component-id (for logging):
    * ot.component.id
    * info.component (if above is missing)
    * manifest (original method, if both are missing)
* Can disable JaxRS Client Setup using ot.client.type=none. Other valid
values (not yet implemented are all, jaxrs, resttemplate)
* Can disable JaxRS Server Setup using ot.server.type=none. Other valid
values (not yet implemented are all, jaxrs, mvc)
    
 
2.12.5
------
* Jmx autoconfiguration can now be disabled with ot.jmx.enabled=false. The reason you might want
that will be explained in your server logs - a set of command line switches might be even easier. We
will add to the wiki article at https://wiki.otcorp.opentable.com/x/YsoIAQ with more details.
* Add an arbitrary amount of time to wait AFTER unnannouncing but BEFORE jetty. The relevant configuration

ot.httpserver.sleep-before-shutdown = true | false (default = false) - whether to sleep at all
ot.httpserver.sleep-duration-before-shutdown = IsoDuration (default = 5 seconds). How long to sleep
    

2.12.4
------
* Uses Jetty 9.4.12.0830 and consumes otj-jaxrs, otj-logging
built on that.

2.12.3
------
* Uses RestEasy beta 5, and consumes otj-jaxrs built on that

2.12.2
------
BAD BUILD

2.12.1
------
BAD BUILD

2.12.0
------
* fix jetty jmx export
* JettyDumper lets you dump jetty state

2.11.1
------
* Mark servlet filter order bean as Optional 

2.11.0
------
* hook SpringApplication.exit to register JvmFallbackShutdown; ICD-945
* support binding connectors to specific network addresses
* use our own server connector rather than Spring Boot's
* OTApplication gets a couple of baseUri helpers

2.10.8
-----
* Logs the GIT commit (and other info) on startup.
* Allow servlet init params to be customized. This allows you to enable RESTEasy's role based auth support.

2.10.7
------
* Added support for dependency-based servlet filter order resolution
  (OTPL-2351).

2.10.6
------
* Added customization hook for web app context.

2.10.5
------
* Fixes bug in `TestMBeanServerConfiguration`.  More detail has also
  been added to the Javadoc.

2.10.4
------
* Spring Boot 2 final and 5.0.4

2.10.3
------
* Fixes an issue introduced in Spring Boot 2.0RC2, which would cause a classloader exception.

2.9.4
-----

* adds tests for jaxrs client referrer logic (otj-jaxrs 2.6.0)

2.9.3
-----

* uses latest parent

2.9.2
-----

* fixes backend info filter for chunked responses

2.9.1
-----

* allow customizing HttpConfiguration

2.9.0
-----

* reverts interface-brekaing change in 2.8.0: restores exposing threadpool size

2.8.1
-----

* import enabling deprecated SSL ciphers from otj-httpserver

2.8.0
-----

* add ability to configure proxy protocol over http(s)

2.7.2
-----

* static path is now configurable

2.7.1
-----

* OTApplication.run now sets the main application class

2.7.0
-----

* rework request logging

2.6.3
-----

* jetty Server is now a @Lazy @Bean

2.6.2
-----

* enable metrics http endpoint

2.6.1
-----

* otj-metrics exposes response count by status code

