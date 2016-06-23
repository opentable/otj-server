TODO Update for Spring.

The `RestHttpServer` provides health checking at `/health` via
`otj-metrics`.

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

----
Copyright (C) 2014 OpenTable, Inc.
