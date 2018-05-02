otj-server
==========

2.11.0
------
* hook SpringApplication.exit to register JvmFallbackShutdown; ICD-945

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

