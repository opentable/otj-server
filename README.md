OpenTable Server Component
==========================

Component Charter
-----------------

* Server startup and shutdown
* Templates that integrate components into useful starting points for service development

Server Main Class
-----------------

`otj-server` provides [StandaloneServer](https://github.com/opentable/otj-server/blob/master/server/src/main/java/com/opentable/server/StandaloneServer.java)
which provides a skeletal server startup and shutdown.

The [AnnouncingStandaloneServer](https://github.com/opentable/otj-server/blob/master/server/src/main/java/com/opentable/server/AnnouncingStandaloneServer.java)
extends this to include automatic service discovery announcements.

The [BasicDiscoveryServerModule](https://github.com/opentable/otj-server/blob/master/templates/src/main/java/com/opentable/server/templates/BasicDiscoveryServerModule.java)
is the actual meat of the server template - it pulls together all of the modules into Guice.

The [BasicRestHttpServerTemplateModule](https://github.com/opentable/otj-server/blob/master/templates/src/main/java/com/opentable/server/templates/BasicRestHttpServerTemplateModule.java) module installs from `otj-metrics` the `HealthHttpModule` by default, which provides a healthcheck endpoint at `/health` for your application.

Historical Note
---------------
Prior to version `1.11.2`, `BasicRestHttpServerTemplateModule` would
install the `otj-metrics` `MetricsHttpModule` at `/metrics` by default.
It no longer does this implicitly.  If you want this functionality, you
must install this module in your service.  See the `otj-metrics`
documentation for more details.

----
Copyright (C) 2014 OpenTable, Inc.
